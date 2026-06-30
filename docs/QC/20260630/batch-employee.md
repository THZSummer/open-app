# 批 employee：员工信息

> 2 文件全部逐行读。

## 文件覆盖表（2/2）
| 文件 | 逐行读 | 问题数 |
|------|:---:|:---:|
| entity/Employee.java(29) | ✅ | 0 |
| mapper/EmployeeMapper.java(31) | ✅ | 0 |

## 意见：无。

## 结论
✅ 通过。EmployeeMapper 支持 selectByWelinkIds（批量）/ selectByWelinkId（单个）/ searchByKeyword（模糊搜索）。规范。
