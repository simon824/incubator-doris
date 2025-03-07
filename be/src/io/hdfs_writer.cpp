// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

#include "io/hdfs_writer.h"

#include <filesystem>

#include "common/logging.h"
#include "service/backend_options.h"

namespace doris {
const static std::string FS_KEY = "fs.defaultFS";
const static std::string USER = "hadoop.username";
const static std::string KERBEROS_PRINCIPAL = "hadoop.kerberos.principal";
const static std::string KERBEROS_KEYTAB = "hadoop.kerberos.keytab";
const static std::string TOKEN = "token";

HDFSWriter::HDFSWriter(std::map<std::string, std::string>& properties, const std::string& path)
        : _properties(properties),
          _path(path),
          _hdfs_fs(nullptr),
          _hdfs_params(_parse_properties(_properties)),
          _builder(createHDFSBuilder(_hdfs_params)) {}

HDFSWriter::~HDFSWriter() {
    close();
}

Status HDFSWriter::open() {
    if (_namenode.empty()) {
        LOG(WARNING) << "hdfs properties is incorrect.";
        return Status::InternalError("hdfs properties is incorrect");
    }
    // if the format of _path is hdfs://ip:port/path, replace it to /path.
    // path like hdfs://ip:port/path can't be used by libhdfs3.
    if (_path.find(_namenode) != _path.npos) {
        _path = _path.substr(_namenode.size());
    }
    RETURN_IF_ERROR(_connect());
    if (_hdfs_fs == nullptr) {
        return Status::InternalError("HDFS writer open without client");
    }
    int exists = hdfsExists(_hdfs_fs, _path.c_str());
    if (exists == 0) {
        // the path already exists
        return Status::AlreadyExist(_path + " already exists.");
    }

    std::filesystem::path hdfs_path(_path);
    std::string hdfs_dir = hdfs_path.parent_path().string();
    exists = hdfsExists(_hdfs_fs, hdfs_dir.c_str());
    if (exists != 0) {
        LOG(INFO) << "hdfs dir doesn't exist, create it: " << hdfs_dir;
        int ret = hdfsCreateDirectory(_hdfs_fs, hdfs_dir.c_str());
        if (ret != 0) {
            std::stringstream ss;
            ss << "create dir failed. "
               << "(BE: " << BackendOptions::get_localhost() << ")"
               << " namenode: " << _namenode << " path: " << hdfs_dir
               << ", err: " << strerror(errno);
            LOG(WARNING) << ss.str();
            return Status::InternalError(ss.str());
        }
    }
    // open file
    _hdfs_file = hdfsOpenFile(_hdfs_fs, _path.c_str(), O_WRONLY, 0, 0, 0);
    if (_hdfs_file == nullptr) {
        std::stringstream ss;
        ss << "open file failed. "
           << "(BE: " << BackendOptions::get_localhost() << ")"
           << " namenode:" << _namenode << " path:" << _path << ", err: " << strerror(errno);
        LOG(WARNING) << ss.str();
        return Status::InternalError(ss.str());
    }
    LOG(INFO) << "open file. namenode:" << _namenode << " path:" << _path;
    return Status::OK();
}

Status HDFSWriter::write(const uint8_t* buf, size_t buf_len, size_t* written_len) {
    if (buf_len == 0) {
        *written_len = 0;
        return Status::OK();
    }
    int32_t result = hdfsWrite(_hdfs_fs, _hdfs_file, buf, buf_len);
    if (result < 0) {
        std::stringstream ss;
        ss << "write file failed. "
           << "(BE: " << BackendOptions::get_localhost() << ")"
           << "namenode:" << _namenode << " path:" << _path << ", err: " << strerror(errno);
        LOG(WARNING) << ss.str();
        return Status::InternalError(ss.str());
    }

    *written_len = (unsigned int)result;
    return Status::OK();
}

Status HDFSWriter::close() {
    if (_closed) {
        return Status::OK();
    }
    _closed = true;
    if (_hdfs_fs == nullptr) {
        return Status::OK();
    }
    if (_hdfs_file == nullptr) {
        // Even if there is an error, the resources associated with the hdfsFS will be freed.
        hdfsDisconnect(_hdfs_fs);
        return Status::OK();
    }
    int result = hdfsFlush(_hdfs_fs, _hdfs_file);
    if (result == -1) {
        std::stringstream ss;
        ss << "failed to flush hdfs file. "
           << "(BE: " << BackendOptions::get_localhost() << ")"
           << "namenode:" << _namenode << " path:" << _path << ", err: " << strerror(errno);
        LOG(WARNING) << ss.str();
        return Status::InternalError(ss.str());
    }
    hdfsCloseFile(_hdfs_fs, _hdfs_file);
    hdfsDisconnect(_hdfs_fs);

    _hdfs_file = nullptr;
    _hdfs_fs = nullptr;
    return Status::OK();
}

Status HDFSWriter::_connect() {
    if (_builder.is_need_kinit()) {
        RETURN_IF_ERROR(_builder.run_kinit());
    }
    _hdfs_fs = hdfsBuilderConnect(_builder.get());
    if (_hdfs_fs == nullptr) {
        std::stringstream ss;
        ss << "connect to hdfs failed. namenode address:" << _namenode << ", error"
           << hdfsGetLastError();
        return Status::InternalError(ss.str());
    }
    return Status::OK();
}

THdfsParams HDFSWriter::_parse_properties(std::map<std::string, std::string>& prop) {
    std::map<std::string, std::string>::iterator iter;
    std::vector<THdfsConf> hdfs_configs;
    THdfsParams hdfsParams;
    for (iter = prop.begin(); iter != prop.end();) {
        if (iter->first.compare(FS_KEY) == 0) {
            _namenode = iter->second;
            hdfsParams.__set_fs_name(_namenode);
            iter = prop.erase(iter);
        } else if (iter->first.compare(USER) == 0) {
            hdfsParams.__set_user(iter->second);
            iter = prop.erase(iter);
        } else if (iter->first.compare(KERBEROS_PRINCIPAL) == 0) {
            hdfsParams.__set_hdfs_kerberos_principal(iter->second);
            iter = prop.erase(iter);
        } else if (iter->first.compare(KERBEROS_KEYTAB) == 0) {
            hdfsParams.__set_hdfs_kerberos_keytab(iter->second);
            iter = prop.erase(iter);
        } else {
            THdfsConf item;
            item.key = iter->first;
            item.value = iter->second;
            hdfs_configs.push_back(item);
            iter = prop.erase(iter);
        }
    }
    hdfsParams.__set_hdfs_conf(hdfs_configs);
    return hdfsParams;
}

} // end namespace doris
