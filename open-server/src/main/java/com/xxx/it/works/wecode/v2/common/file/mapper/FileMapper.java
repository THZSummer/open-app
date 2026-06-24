package com.xxx.it.works.wecode.v2.common.file.mapper;

import com.xxx.it.works.wecode.v2.common.file.entity.FileEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文件 Mapper
 *
 * @author SDDU Build Agent
 * @date 2026-06-07
 */
@Mapper
public interface FileMapper {

    FileEntity selectByFileId(@Param("fileId") String fileId);

    List<FileEntity> selectByFileIds(@Param("fileIds") List<String> fileIds);

    int insert(FileEntity file);
}
