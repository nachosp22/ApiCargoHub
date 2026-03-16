// Preload script — runs in renderer process context with Node.js access
// Use contextBridge to safely expose APIs to the renderer

import { contextBridge } from 'electron'

// Expose protected methods that allow the renderer process to use
// specific Electron/Node APIs without exposing the entire API surface
contextBridge.exposeInMainWorld('electronAPI', {
  platform: process.platform,
  versions: {
    node: process.versions.node,
    chrome: process.versions.chrome,
    electron: process.versions.electron,
  },
})
