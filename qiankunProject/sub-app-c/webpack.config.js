const path = require('path');
const HtmlWebpackPlugin = require('html-webpack-plugin');

// Webpack 子应用 C 配置
module.exports = {
  entry: './src/main.jsx',
  output: {
    path: path.resolve(__dirname, 'dist'),
    filename: 'bundle.js',
    // 导出为 umd 格式，供主应用 qiankun 获取子应用导出的生命周期
    library: 'sub-app-c',
    libraryTarget: 'umd',
    // 避免与其它子应用的 chunk 加载全局变量冲突
    chunkLoadingGlobal: 'webpackJsonp_sub_app_c'
  },
  resolve: {
    extensions: ['.js', '.jsx']
  },
  devServer: {
    // 使用自定义域名，便于跨子应用 cookie / 域名隔离
    host: 'localhost.uat.com',
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
        test: /\.jsx?$/,
        exclude: /node_modules/,
        use: {
          loader: 'babel-loader',
          options: {
            presets: ['@babel/preset-env', '@babel/preset-react']
          }
        }
      },
      {
        test: /\.css$/,
        use: ['style-loader', 'css-loader']
      }
    ]
  },
  plugins: [
    new HtmlWebpackPlugin({
      template: './index.html'
    })
  ]
};
