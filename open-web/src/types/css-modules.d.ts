// CSS Modules 类型声明
declare module '*.module.css' {
  const classes: { [key: string]: string };
  export default classes;
}

declare module '*.module.scss' {
  const classes: { [key: string]: string };
  export default classes;
}

declare module '*.module.less' {
  const classes: { [key: string]: string };
  export default classes;
}

declare module '*.m.css' {
  const classes: { [key: string]: string };
  export default classes;
}

declare module '*.m.scss' {
  const classes: { [key: string]: string };
  export default classes;
}

declare module '*.m.less' {
  const classes: { [key: string]: string };
  export default classes;
}

// 普通样式文件
declare module '*.css' {
  const content: string;
  export default content;
}

declare module '*.scss' {
  const content: string;
  export default content;
}

declare module '*.less' {
  const content: string;
  export default content;
}
