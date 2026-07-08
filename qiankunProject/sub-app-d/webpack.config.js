const path = require('path');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const { VueLoaderPlugin } = require('vue-loader');

// Webpack 子应用 D 配置（Vue 3）
module.exports = {
  entry: './src/main.js',
  output: {
    path: path.resolve(__dirname, 'dist'),
    filename: 'bundle.js',
    // 导出为 umd 格式，供主应用 qiankun 获取子应用导出的生命周期
    library: 'sub-app-d',
    libraryTarget: 'umd',
    // 避免与其它子应用的 chunk 加载全局变量冲突
    chunkLoadingGlobal: 'webpackJsonp_sub_app_d'
  },
  resolve: {
    extensions: ['.js', '.vue']
  },
  devServer: {
    port: 8082,
    // 允许主应用跨域拉取子应用资源
    headers: {
      'Access-Control-Allow-Origin': '*'
    },
    historyApiFallback: true,
    hot: true
  },
  module: {
    rules: [
      {
        // 处理 .vue 单文件组件
        test: /\.vue$/,
        loader: 'vue-loader'
      },
      {
        test: /\.js$/,
        exclude: /node_modules/,
        use: {
          loader: 'babel-loader',
          options: {
            presets: ['@babel/preset-env']
          }
        }
      },
      {
        test: /\.css$/,
        use: ['vue-style-loader', 'css-loader']
      }
    ]
  },
  plugins: [
    new VueLoaderPlugin(),
    new HtmlWebpackPlugin({
      template: './index.html'
    })
  ]
};
