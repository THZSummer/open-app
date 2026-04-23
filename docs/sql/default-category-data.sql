-- ============================================================================
-- 能力开放平台 - 分类表默认数据
-- 版本: v1.0
-- 导出日期: 2026-04-23
-- 说明: 包含系统默认的分类树结构数据
-- 记录数: 13条
-- ============================================================================
-- MySQL dump 10.13  Distrib 8.0.45, for Linux (x86_64)
--
-- Host: localhost    Database: openapp
-- ------------------------------------------------------
-- Server version	8.0.45-0ubuntu0.24.04.1

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Dumping data for table `openplatform_v2_category_t`
--

LOCK TABLES `openplatform_v2_category_t` WRITE;
/*!40000 ALTER TABLE `openplatform_v2_category_t` DISABLE KEYS */;
INSERT INTO `openplatform_v2_category_t` (`id`, `category_alias`, `name_cn`, `name_en`, `parent_id`, `path`, `sort_order`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES (305650853237227520,'api_business_app_soa','API-业务应用-应用身份-SOA','appsoa',NULL,'/305650853237227520/',1,1,'2026-04-23 10:27:22.130','2026-04-23 14:01:08.632','system','system'),(305650973508894720,'api_business_app_apig','API-业务应用-应用身份-APIG','appapig',NULL,'/305650973508894720/',2,1,'2026-04-23 10:27:50.805','2026-04-23 14:01:44.314','system','system'),(305652258039660544,NULL,'消息','IM',305650853237227520,'/305650853237227520/305652258039660544/',0,1,'2026-04-23 10:32:57.061','2026-04-23 10:32:57.061','system','system'),(305652314155253760,NULL,'会议','Metting',305650853237227520,'/305650853237227520/305652314155253760/',0,1,'2026-04-23 10:33:10.440','2026-04-23 10:33:10.440','system','system'),(305652407616929792,NULL,'云盘','CloudBox',305650973508894720,'/305650973508894720/305652407616929792/',0,1,'2026-04-23 10:33:32.723','2026-04-23 10:33:32.723','system','system'),(305653292027871232,'event','事件','event',NULL,'/305653292027871232/',9,1,'2026-04-23 10:37:03.583','2026-04-23 11:23:57.146','system','system'),(305654385424203776,'callback','回调','callback',NULL,'/305654385424203776/',6,1,'2026-04-23 10:41:24.269','2026-04-23 11:24:21.441','system','system'),(305654654555914240,'api_personal_user_aksk','API-个人应用-用户身份-AKSK','persional_aksk',NULL,'/305654654555914240/',5,1,'2026-04-23 10:42:28.435','2026-04-23 14:02:13.027','system','system'),(305654770410979328,NULL,'文档','Doc',305654654555914240,'/305654654555914240/305654770410979328/',0,1,'2026-04-23 10:42:56.057','2026-04-23 10:43:18.919','system','system'),(305664724417118208,'api_business_user_soa','API-业务应用-用户身份-SOA','api_business_user_soa',NULL,'/305664724417118208/',3,1,'2026-04-23 11:22:29.277','2026-04-23 14:01:52.852','system','system'),(305664845166936064,'api_business_user_apig','API-业务应用-用户身份-APIG','api_business_user_apig',NULL,'/305664845166936064/',4,1,'2026-04-23 11:22:58.113','2026-04-23 14:02:04.631','system','system'),(305665045520989184,NULL,'日程','Schedule',305664724417118208,'/305664724417118208/305665045520989184/',0,1,'2026-04-23 11:23:45.910','2026-04-23 11:23:45.910','system','system'),(305665072953942016,NULL,'邮件','Mail',305664845166936064,'/305664845166936064/305665072953942016/',0,1,'2026-04-23 11:24:09.400','2026-04-23 11:24:09.400','system','system');
/*!40000 ALTER TABLE `openplatform_v2_category_t` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-04-23 14:25:52
