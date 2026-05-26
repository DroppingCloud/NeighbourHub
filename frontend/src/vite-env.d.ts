// 手动声明 Vite 的 ImportMeta 类型，避免依赖 vite 包已安装
// 运行时由 Vite 注入，这里仅供 TypeScript 类型检查使用

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string
  // 按需扩展其他环境变量
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
