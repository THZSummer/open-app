/**
 * ========================================
 * 流程工具函数库
 * ========================================
 * 
 * 提供连接器和连接流管理功能所需的核心工具函数
 * 包括节点ID生成、位置计算、验证等功能
 */

/**
 * 生成唯一的节点ID
 * @param {string} prefix - ID前缀，默认为'node'
 * @returns {string} 格式：prefix_timestamp_randomString
 * @example generateNodeId('trigger') => "trigger_1715000000000_abc123xyz"
 */
export const generateNodeId = (prefix = 'node') => {
  const timestamp = Date.now();
  const randomStr = Math.random().toString(36).substr(2, 9);
  return `${prefix}_${timestamp}_${randomStr}`;
};

/**
 * 生成唯一的连线ID
 * @returns {string} 格式：edge_timestamp_randomString
 */
export const generateEdgeId = () => {
  const timestamp = Date.now();
  const randomStr = Math.random().toString(36).substr(2, 9);
  return `edge_${timestamp}_${randomStr}`;
};

/**
 * 初始化节点位置（网格对齐）
 * 将节点位置对齐到20px的网格
 * @param {number} x - X坐标
 * @param {number} y - Y坐标
 * @returns {{x: number, y: number}} 对齐后的坐标
 */
export const getInitialNodePosition = (x = 100, y = 100) => ({
  x: Math.round(x / 20) * 20,
  y: Math.round(y / 20) * 20,
});

/**
 * 节点类型常量
 * 定义支持的6种节点类型
 */
export const NODE_TYPES = {
  TRIGGER: 'trigger',    // 触发器节点
  ACTION: 'action',      // 执行动作节点
  CONDITION: 'condition', // 条件分支节点
  DELAY: 'delay',        // 延时节点
  PARALLEL: 'parallel',  // 并行执行节点
  LOOP: 'loop',          // 循环执行节点
};

/**
 * 流程类型常量
 * 定义3种流程类型
 */
export const FLOW_TYPES = {
  BUSINESS: 'business',  // 业务流（事件驱动）
  SCHEDULE: 'schedule',  // 定时流
  SUBFLOW: 'subflow',     // 子流程
};

/**
 * 验证节点配置
 * @param {Object} node - 节点对象
 * @returns {{valid: boolean, errors: Array}} 验证结果
 */
export const validateNodeConfig = (node) => {
  const errors = [];
  const { type, data } = node;
  
  switch (type) {
    case NODE_TYPES.TRIGGER:
      if (!data.config?.triggerType) {
        errors.push('请选择触发类型');
      }
      if (data.config?.triggerType === 'schedule' && !data.config?.cronExpression) {
        errors.push('定时触发需要配置Cron表达式');
      }
      if (data.config?.triggerType === 'webhook' && !data.config?.webhookPath) {
        errors.push('Webhook触发需要配置Webhook路径');
      }
      break;
      
    case NODE_TYPES.ACTION:
      if (!data.config?.connectorId) {
        errors.push('请选择连接器');
      }
      if (!data.config?.actionId) {
        errors.push('请选择执行动作');
      }
      break;
      
    case NODE_TYPES.CONDITION:
      if (!data.config?.conditions || data.config.conditions.length === 0) {
        errors.push('请至少添加一个条件');
      }
      break;
      
    case NODE_TYPES.PARALLEL:
      if (!data.config?.branches || data.config.branches.length < 2) {
        errors.push('并行执行至少需要2个分支');
      }
      break;
      
    case NODE_TYPES.LOOP:
      if (data.config?.loopType === 'times') {
        if (!data.config?.maxIterations || data.config.maxIterations < 1) {
          errors.push('循环次数必须大于0');
        }
        if (data.config.maxIterations > 1000) {
          errors.push('循环次数不能超过1000');
        }
      }
      break;
      
    case NODE_TYPES.DELAY:
      if (!data.config?.duration || data.config.duration < 1) {
        errors.push('延时时长必须大于0秒');
      }
      if (data.config?.duration > 86400) {
        errors.push('延时时长不能超过24小时（86400秒）');
      }
      break;
  }
  
  return {
    valid: errors.length === 0,
    errors,
  };
};

/**
 * 验证流程配置
 * 检查流程是否包含必需的节点和有效的连线
 * @param {Array} nodes - 节点数组
 * @param {Array} edges - 连线数组
 * @returns {{valid: boolean, errors: Array, warnings: Array}} 验证结果
 */
export const validateFlowConfig = (nodes = [], edges = []) => {
  const errors = [];
  const warnings = [];
  
  // 检查是否有触发器节点
  const hasTrigger = nodes.some(node => node.type === NODE_TYPES.TRIGGER);
  if (!hasTrigger) {
    errors.push('流程必须包含至少一个触发器节点');
  }
  
  // 检查是否有孤立节点
  const connectedNodeIds = new Set();
  edges.forEach(edge => {
    connectedNodeIds.add(edge.source);
    connectedNodeIds.add(edge.target);
  });
  
  nodes.forEach(node => {
    if (node.type !== NODE_TYPES.TRIGGER && !connectedNodeIds.has(node.id)) {
      warnings.push(`节点 "${node.data?.label || node.id}" 未连接到流程`);
    }
  });
  
  // 检查每个节点的配置
  nodes.forEach(node => {
    const nodeValidation = validateNodeConfig(node);
    if (!nodeValidation.valid) {
      nodeValidation.errors.forEach(error => {
        errors.push(`节点 "${node.data?.label || node.id}"：${error}`);
      });
    }
  });
  
  // 检查连线是否有效
  edges.forEach(edge => {
    const sourceNode = nodes.find(n => n.id === edge.source);
    const targetNode = nodes.find(n => n.id === edge.target);
    
    if (!sourceNode) {
      errors.push(`连线起点 "${edge.source}" 不存在`);
    }
    if (!targetNode) {
      errors.push(`连线终点 "${edge.target}" 不存在`);
    }
  });
  
  return {
    valid: errors.length === 0,
    errors,
    warnings,
  };
};

/**
 * 深拷贝流程配置
 * 用于保存前的数据备份
 * @param {Object} flowConfig - 流程配置对象
 * @returns {Object} 深拷贝后的对象
 */
export const cloneFlowConfig = (flowConfig) => {
  return JSON.parse(JSON.stringify(flowConfig));
};

/**
 * 计算画布边界
 * 用于自动缩放和居中显示
 * @param {Array} nodes - 节点数组
 * @param {number} padding - 内边距
 * @returns {{minX, minY, maxX, maxY, width, height}} 边界信息
 */
export const calculateBounds = (nodes, padding = 50) => {
  if (!nodes || nodes.length === 0) {
    return {
      minX: 0,
      minY: 0,
      maxX: 800,
      maxY: 600,
      width: 800,
      height: 600
    };
  }
  
  let minX = Infinity;
  let minY = Infinity;
  let maxX = -Infinity;
  let maxY = -Infinity;
  
  nodes.forEach(node => {
    const x = node.position?.x || 0;
    const y = node.position?.y || 0;
    const width = node.width || 150;
    const height = node.height || 60;
    
    minX = Math.min(minX, x);
    minY = Math.min(minY, y);
    maxX = Math.max(maxX, x + width);
    maxY = Math.max(maxY, y + height);
  });
  
  return {
    minX: minX - padding,
    minY: minY - padding,
    maxX: maxX + padding,
    maxY: maxY + padding,
    width: maxX - minX + padding * 2,
    height: maxY - minY + padding * 2
  };
};

/**
 * 生成流程图的缩略图Data URL
 * 
 * @param {Array} nodes - 节点列表
 * @param {Array} edges - 连线列表
 * @param {number} width - 缩略图宽度，默认200
 * @param {number} height - 缩略图高度，默认150
 * @returns {Promise<string|null>} Base64编码的图片Data URL
 */
export const generateFlowThumbnail = async (nodes = [], edges = [], width = 200, height = 150) => {
  // 如果没有节点，返回null
  if (!nodes || nodes.length === 0) {
    return null;
  }

  try {
    // 计算所有节点的边界
    const padding = 40;
    let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
    
    nodes.forEach(node => {
      const x = node.position?.x || 0;
      const y = node.position?.y || 0;
      minX = Math.min(minX, x);
      minY = Math.min(minY, y);
      maxX = Math.max(maxX, x + 150);
      maxY = Math.max(maxY, y + 60);
    });
    
    // 添加padding
    minX -= padding;
    minY -= padding;
    maxX += padding;
    maxY += padding;
    
    // 计算原始尺寸
    const originalWidth = maxX - minX;
    const originalHeight = maxY - minY;
    
    // 创建Canvas
    const canvas = document.createElement('canvas');
    canvas.width = width;
    canvas.height = height;
    
    const ctx = canvas.getContext('2d');
    
    // 填充白色背景
    ctx.fillStyle = '#ffffff';
    ctx.fillRect(0, 0, width, height);
    
    // 计算缩放比例，保持宽高比
    const scaleX = width / originalWidth;
    const scaleY = height / originalHeight;
    const scale = Math.min(scaleX, scaleY, 1);
    
    // 居中偏移
    const offsetX = (width - originalWidth * scale) / 2 - minX * scale;
    const offsetY = (height - originalHeight * scale) / 2 - minY * scale;
    
    // 应用变换
    ctx.save();
    ctx.translate(offsetX, offsetY);
    ctx.scale(scale, scale);
    
    // 绘制连线
    ctx.strokeStyle = '#1890ff';
    ctx.lineWidth = 2 / scale;
    
    edges.forEach(edge => {
      const sourceNode = nodes.find(n => n.id === edge.source);
      const targetNode = nodes.find(n => n.id === edge.target);
      
      if (sourceNode && targetNode) {
        const sourceX = sourceNode.position.x + 75;
        const sourceY = sourceNode.position.y + 60;
        const targetX = targetNode.position.x + 75;
        const targetY = targetNode.position.y;
        
        ctx.beginPath();
        ctx.moveTo(sourceX, sourceY);
        // 贝塞尔曲线连接
        const midY = (sourceY + targetY) / 2;
        ctx.bezierCurveTo(sourceX, midY, targetX, midY, targetX, targetY);
        ctx.stroke();
      }
    });
    
    // 节点颜色映射
    const nodeColors = {
      trigger: { bg: '#e6f7ff', border: '#1890ff' },
      action: { bg: '#f6ffed', border: '#52c41a' },
      condition: { bg: '#fffbe6', border: '#faad14' },
      delay: { bg: '#f9f0ff', border: '#722ed1' },
      parallel: { bg: '#e6fffb', border: '#13c2c2' },
      loop: { bg: '#fff0f3', border: '#eb2f96' },
    };
    
    // 绘制节点
    nodes.forEach(node => {
      const colors = nodeColors[node.type] || { bg: '#f5f5f5', border: '#d9d9d9' };
      const x = node.position?.x;
      const y = node.position?.y;
      const nodeWidth = 150;
      const nodeHeight = 60;
      
      // 节点背景
      ctx.fillStyle = colors.bg;
      ctx.strokeStyle = colors.border;
      ctx.lineWidth = 2 / scale;
      
      // 圆角矩形
      const radius = 6;
      ctx.beginPath();
      ctx.moveTo(x + radius, y);
      ctx.lineTo(x + nodeWidth - radius, y);
      ctx.quadraticCurveTo(x + nodeWidth, y, x + nodeWidth, y + radius);
      ctx.lineTo(x + nodeWidth, y + nodeHeight - radius);
      ctx.quadraticCurveTo(x + nodeWidth, y + nodeHeight, x + nodeWidth - radius, y + nodeHeight);
      ctx.lineTo(x + radius, y + nodeHeight);
      ctx.quadraticCurveTo(x, y + nodeHeight, x, y + nodeHeight - radius);
      ctx.lineTo(x, y + radius);
      ctx.quadraticCurveTo(x, y, x + radius, y);
      ctx.closePath();
      ctx.fill();
      ctx.stroke();
      
      // 节点名称
      ctx.fillStyle = '#333';
      ctx.font = `${14 / scale}px Arial`;
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      
      const label = node.data?.label || node.type;
      const maxWidth = nodeWidth - 20;
      
      // 文字截断
      let displayLabel = label;
      if (ctx.measureText(label).width > maxWidth) {
        while (ctx.measureText(displayLabel + '...').width > maxWidth && displayLabel.length > 0) {
          displayLabel = displayLabel.slice(0, -1);
        }
        displayLabel += '...';
      }
      
      ctx.fillText(displayLabel, x + nodeWidth / 2, y + nodeHeight / 2);
    });
    
    ctx.restore();
    
    // 转换为Data URL
    return canvas.toDataURL('image/png');
  } catch (error) {
    console.error('生成流程图缩略图失败:', error);
    return null;
  }
};

/**
 * 性能测量工具类
 */
export class PerformanceMonitor {
  constructor(label) {
    this.label = label;
    this.startTime = 0;
  }

  start() {
    this.startTime = performance.now();
  }

  end() {
    const duration = performance.now() - this.startTime;
    console.log(`[Performance] ${this.label}: ${duration.toFixed(2)}ms`);
    return duration;
  }

  static measure(label, fn) {
    const monitor = new PerformanceMonitor(label);
    monitor.start();
    const result = fn();
    monitor.end();
    return result;
  }
}

/**
 * 节流函数
 * @param {Function} fn - 要节流的函数
 * @param {number} delay - 延迟时间（毫秒）
 * @returns {Function} 节流后的函数
 */
export const throttle = (fn, delay) => {
  let lastCall = 0;
  return (...args) => {
    const now = Date.now();
    if (now - lastCall >= delay) {
      lastCall = now;
      return fn(...args);
    }
  };
};

/**
 * 防抖函数
 * @param {Function} fn - 要防抖的函数
 * @param {number} delay - 延迟时间（毫秒）
 * @returns {Function} 防抖后的函数
 */
export const debounce = (fn, delay) => {
  let timeoutId;
  return (...args) => {
    clearTimeout(timeoutId);
    timeoutId = setTimeout(() => fn(...args), delay);
  };
};
