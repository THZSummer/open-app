---
name: project-documenter
description: "Documents project structure, styles, layouts, and interactions into a comprehensive specification. Invoke when user asks to document/replicate a project or generate a specification document for recreating the project."
---

# Project Documenter

This skill analyzes the current project and generates a comprehensive documentation file that captures all aspects needed to recreate the project in a new environment.

## When to Use

- User asks to document the current project
- User wants to replicate/generate the project elsewhere
- User needs a complete specification of project structure, styles, and interactions
- User wants to create a new project based on an existing one's design

## Confirmation Workflow (IMPORTANT)

Before and during the documentation process, you MUST confirm with the user at each stage:

### Stage 1: Initial Analysis Confirmation
Before scanning the project, ask the user:
- What is the purpose/context of documenting this project?
- Are there any specific areas/pages/components the you want to focus on or exclude?
- Any particular details you want prioritized in the documentation?

### Stage 2: Directory Structure Confirmation
After scanning the directory structure but before including it in the documentation:
- Present the filtered directory structure (excluding .trae, dist, node_modules, etc.)
- Confirm with user if this structure is correct and complete
- Ask if any additional files/directories should be excluded or included

### Stage 3: Page/Component Documentation Confirmation
For each page or significant component:
- Present the analysis of that specific page/component
- Confirm the understanding of its purpose, layout, style, and interactions
- Get user confirmation before moving to the next page/component

### Stage 4: Final Document Confirmation
Before writing the final specification document:
- Present the complete structure of the document
- Confirm with user that all sections are correct
- Only then generate the specification file

## Documentation Output

The generated document will include:

1. **Directory Structure**: Complete file tree (excluding .trae, dist, node_modules, build artifacts, cache files, etc.) with descriptions for each directory and file
2. **Pages Layout & Interactions**:
   - For each page/route:
     - Page purpose and description
     - Page layout structure (components hierarchy and positioning)
     - Visual elements and their arrangement
     - User interactions (click, hover, navigation, form inputs)
     - State changes and animations
     - Routing behavior
3. **Components Documentation**:
   - For each significant component:
     - Component purpose and description
     - Layout structure within the component
     - Style specifications (colors, typography, spacing)
     - Interaction behaviors and event handlers

## Implementation Steps

### Step 1: Pre-Analysis Confirmation (REQUIRED)
Before scanning the project:
- Ask the user about the purpose of documentation
- Identify any specific focus areas or exclusions
- Confirm understanding of user expectations

### Step 2: Scan and Filter Directory Structure
- List all directories and files recursively
- Filter out non-project files: .trae, dist, node_modules, build, .cache, .git (optional), environment files, etc.
- Present the filtered structure to user for confirmation
- Adjust based on user feedback

### Step 3: Analyze Each Page/Component (with Confirmation)
For each significant page and component:

#### Page Documentation Template:
```
### [Page Name]
- **File Path**: [exact file path]
- **Purpose**: [what this page does]
- **Route**: [URL route if applicable]

#### Layout Structure
[ASCII diagram showing the layout hierarchy]
Example:
┌─────────────────────────────────────┐
│           Header (64px)             │
├─────────────────────────────────────┤
│         SubHeader (optional)        │
├────────────┬────────────────────────┤
│  Sidebar   │      Content Area      │
│  (220px)   │                        │
│            │                        │
└────────────┴────────────────────────┘

#### Internal Layout Details
[必须详细描述页面内部每个区域的布局结构，包括:
- 外层容器样式 (背景、边框、圆角、内边距)
- 头部区域 (标题、描述、按钮的排列方式)
- 内容区域 (表格、表单、卡片等的排列)
- 底部区域 (分页、操作按钮等)
]

#### Visual Elements
- [List all visual elements: buttons, inputs, cards, tables, etc.]
- [Describe their arrangement and positioning]

#### Style Specifications
- **Colors**: [background, primary, text colors]
- **Typography**: [font sizes, weights for headings/body]
- **Spacing**: [padding, margin values]
- **Dimensions**: [fixed heights, widths if applicable]

#### Interactions
- **Click**: [what happens on click]
- **Hover**: [hover states if any]
- **Navigation**: [route changes, URL parameters]
- **Forms**: [input handling, validation]
- **State Changes**: [loading, error, empty states]

#### Routing Behavior
- [URL parameters used]
- [Query strings]
- [Navigation guards if any]
```

#### Component Documentation Template:
```
### [Component Name]
- **File Path**: [exact file path]
- **Purpose**: [what this component does]

#### Layout Structure
[ASCII diagram of internal layout]

#### Internal Layout Details
[必须详细描述组件内部布局结构，包括:
- 整体容器 (Drawer/Modal/Card等)
- 头部区域 (标题、操作按钮)
- 主体区域 (表单、表格、列表等)
- 底部区域 (确认/取消按钮、分页等)
- 各区域之间的排列方式和间距
]

#### Style Specifications
- **Colors**: [colors used]
- **Typography**: [text styles]
- **Spacing**: [internal spacing]
- **Dimensions**: [width, height, min-height等]

#### Props/Parameters
- [List all props and their purposes]

#### Interactions
- **Events**: [what events it emits/handles]
- **Callbacks**: [onClick, onChange, etc.]
- **State Changes**: [internal state changes]
```

After documenting each page/component:
- Present the analysis to user
- Confirm understanding is correct
- Make adjustments if needed
- Move to next page/component only after confirmation

### Step 4: Generate Specification Document
- Organize all confirmed content
- Create the specification document
- Present structure to user for final confirmation
- Write to file only after user approval

## Output Format

The documentation will be written to `.trae/skills/project-documenter/output/<timestamp>-specification.md`

## Example Usage

When user says "document this project" or "create a spec for recreating this project", invoke this skill.

## Quality Standards

- Provide complete directory structure with clear descriptions for each file and folder (excluding non-project files)
- Document each page with clear layout structure and component hierarchy
- Include ASCII layout diagrams for all pages and significant components
- Detail all user interactions including click, hover, navigation, and form behaviors
- Include visual descriptions of elements and their positioning
- Reference actual file paths for all documented items
- Use diagrams and tables where helpful to illustrate layout and structure
- **CRITICAL**: Confirm with user at each stage before proceeding

## Documentation Rules (IMPORTANT)

When generating documentation, the following rules MUST be followed:

### Rule 1: Layout Components
- **DO NOT** use Ant Design's `Row`, `Col`, `Space`, or `Typography` components for layout and typography
- Use standard HTML elements (div, span, etc.) with CSS/Less for layout instead
- Use native CSS flexbox or grid for positioning elements

### Rule 2: Pagination
- **DO NOT** use Ant Design Table's built-in pagination
- Always use separate `Pagination` component from Ant Design
- Pagination must include:
  - `showQuickJumper`: Enable quick page number input
  - `showSizeChanger`: Enable page size selector
  - `pageSizeOptions`: `[10, 20, 50]`
  - Default `pageSize`: 10
- Place the Pagination component below the Table, outside of the Table component

Example:
```jsx
<Table columns={columns} dataSource={data} rowKey="id" pagination={false} />
<div style={{ marginTop: 16, textAlign: 'right' }}>
  <Pagination
    total={total}
    current={currentPage}
    pageSize={pageSize}
    pageSizeOptions={[10, 20, 50]}
    showSizeChanger
    showQuickJumper
    showTotal={(total) => `共 ${total} 条`}
    onChange={handlePageChange}
  />
</div>
```