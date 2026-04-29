#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
检查并修复Java文件中的"注释前空行"规范问题
"""

import os
import re
import sys
from pathlib import Path
from typing import List, Tuple, Dict

class JavaCommentChecker:
    """Java注释前空行检查器"""
    
    def __init__(self):
        self.violations = []  # 存储所有违规情况
        
    def is_code_line(self, line: str) -> bool:
        """判断是否为代码行（非空行，非纯注释行）"""
        stripped = line.strip()
        if not stripped:
            return False
        # 排除纯注释行（包括文档注释）
        if stripped.startswith('//') or stripped.startswith('/*') or stripped.startswith('*'):
            return False
        return True
    
    def is_comment_line(self, line: str) -> bool:
        """判断是否为注释行（单行注释或块注释开始）"""
        stripped = line.strip()
        if not stripped:
            return False
        # 单行注释
        if stripped.startswith('//'):
            return True
        # 块注释开始（但不是文档注释 /**）
        if stripped.startswith('/*') and not stripped.startswith('/**'):
            return True
        return False
    
    def is_inline_comment(self, line: str) -> bool:
        """判断行尾是否有内联注释（代码后面紧跟注释）"""
        stripped = line.strip()
        if not stripped:
            return False
        # 检查是否有 // 但不是行首
        if '//' in stripped and not stripped.startswith('//'):
            # 排除字符串中的 //
            # 简单处理：检查 // 前面是否有代码
            idx = stripped.find('//')
            if idx > 0:
                # 检查是否在字符串中（简单判断）
                before = stripped[:idx]
                # 如果前面有非空白字符，认为是内联注释
                if before.strip():
                    return True
        return False
    
    def check_file(self, file_path: str) -> List[Tuple[int, str, str]]:
        """
        检查单个文件
        返回: [(行号, 违规类型, 代码行), ...]
        """
        violations = []
        
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                lines = f.readlines()
        except Exception as e:
            print(f"  ⚠️  无法读取文件 {file_path}: {e}")
            return violations
        
        for i in range(len(lines) - 1):
            current_line = lines[i].rstrip('\n\r')
            next_line = lines[i + 1].rstrip('\n\r')
            
            # 情况1：代码行后面紧跟注释行
            if self.is_code_line(current_line) and self.is_comment_line(next_line):
                violations.append((i + 2, 'CODE_TO_COMMENT', current_line, next_line))
            
            # 情况2：代码行内有内联注释
            # 注意：这种情况不需要修复，因为注释在代码行的末尾
            
            # 情况3：右大括号后紧跟注释（但不是文档注释）
            if current_line.strip().endswith('}') and self.is_comment_line(next_line):
                # 检查是否是类/方法结尾后紧跟注释
                # 如果当前行只有 }，则认为是代码块的结束
                if current_line.strip() == '}':
                    violations.append((i + 2, 'BRACE_TO_COMMENT', current_line, next_line))
        
        return violations
    
    def fix_file(self, file_path: str, violations: List[Tuple]) -> bool:
        """
        修复文件中的违规情况
        返回: 是否进行了修复
        """
        if not violations:
            return False
        
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                lines = f.readlines()
        except Exception as e:
            print(f"  ⚠️  无法读取文件 {file_path}: {e}")
            return False
        
        # 从后往前插入空行，避免行号变化
        violations_sorted = sorted(violations, key=lambda x: x[0], reverse=True)
        
        for violation in violations_sorted:
            line_num = violation[0]  # 注释行的行号（1-based）
            # 在注释行之前插入空行
            # line_num是1-based，转换为0-based索引需要-1
            # 我们要在 line_num-1 的位置插入（即当前行的前面）
            insert_index = line_num - 1
            lines.insert(insert_index, '\n')
        
        try:
            with open(file_path, 'w', encoding='utf-8') as f:
                f.writelines(lines)
            return True
        except Exception as e:
            print(f"  ⚠️  无法写入文件 {file_path}: {e}")
            return False
    
    def scan_project(self, project_path: str, fix: bool = False) -> Dict:
        """
        扫描项目
        返回: {
            'total_files': 总文件数,
            'violations_count': 违规数量,
            'violations': [(文件路径, 行号, 违规类型), ...],
            'fixed_files': 修复的文件数
        }
        """
        result = {
            'total_files': 0,
            'violations_count': 0,
            'violations': [],
            'fixed_files': 0
        }
        
        # 查找所有Java文件
        java_files = list(Path(project_path).rglob('*.java'))
        result['total_files'] = len(java_files)
        
        for java_file in java_files:
            file_path = str(java_file)
            violations = self.check_file(file_path)
            
            if violations:
                result['violations_count'] += len(violations)
                for v in violations:
                    result['violations'].append((file_path, v[0], v[1], v[2], v[3]))
                
                if fix:
                    if self.fix_file(file_path, violations):
                        result['fixed_files'] += 1
                        print(f"  ✓ 修复: {java_file.name} ({len(violations)}处)")
        
        return result

def main():
    """主函数"""
    projects = [
        '/home/usb/workspace/wks-open-app/open-app/open-server',
        '/home/usb/workspace/wks-open-app/open-app/api-server',
        '/home/usb/workspace/wks-open-app/open-app/event-server'
    ]
    
    checker = JavaCommentChecker()
    
    print("=" * 80)
    print("第一阶段：检查违规情况")
    print("=" * 80)
    
    all_results = []
    
    for project_path in projects:
        project_name = Path(project_path).name
        print(f"\n📁 扫描项目: {project_name}")
        
        result = checker.scan_project(project_path, fix=False)
        all_results.append((project_name, result))
        
        print(f"  文件总数: {result['total_files']}")
        print(f"  违规数量: {result['violations_count']}")
    
    # 输出详细的违规信息
    print("\n" + "=" * 80)
    print("违规详情")
    print("=" * 80)
    
    for project_name, result in all_results:
        if result['violations']:
            print(f"\n📁 {project_name}:")
            for file_path, line_num, violation_type, code_line, comment_line in result['violations']:
                rel_path = Path(file_path).relative_to(Path(project_path).parent)
                print(f"  {rel_path}:{line_num}")
                print(f"    代码: {code_line.strip()}")
                print(f"    注释: {comment_line.strip()}")
    
    # 统计汇总
    print("\n" + "=" * 80)
    print("检查统计")
    print("=" * 80)
    
    total_violations = sum(r['violations_count'] for _, r in all_results)
    
    for project_name, result in all_results:
        print(f"  {project_name}: {result['violations_count']} 处违规")
    
    print(f"\n  总计: {total_violations} 处违规")
    
    # 如果有违规，询问是否修复
    if total_violations > 0:
        print("\n" + "=" * 80)
        print("第二阶段：修复违规")
        print("=" * 80)
        
        for project_path in projects:
            project_name = Path(project_path).name
            print(f"\n📁 修复项目: {project_name}")
            
            result = checker.scan_project(project_path, fix=True)
            
            print(f"  修复文件: {result['fixed_files']}")
    
    print("\n" + "=" * 80)
    print("完成！")
    print("=" * 80)

if __name__ == '__main__':
    main()
