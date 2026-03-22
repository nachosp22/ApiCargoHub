/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<object, object, unknown>
  export default component
}

interface ElectronAPI {
  platform: string
  versions: {
    node: string
    chrome: string
    electron: string
  }
}

interface Window {
  electronAPI?: ElectronAPI
}

interface ImportMetaEnv {
  readonly VITE_FEATURE_FLEET_REALTIME?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
