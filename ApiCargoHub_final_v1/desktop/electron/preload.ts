// Preload script — runs in renderer process context with Node.js access
// Use contextBridge to safely expose APIs to the renderer

import { contextBridge, ipcRenderer } from 'electron'

// Expose protected methods that allow the renderer process to use
// specific Electron/Node APIs without exposing the entire API surface
const electronBridge = {
  platform: process.platform,
  versions: {
    node: process.versions.node,
    chrome: process.versions.chrome,
    electron: process.versions.electron,
  },
  minimizeWindow: () => ipcRenderer.invoke('window:minimize'),
  toggleMaximizeWindow: () => ipcRenderer.invoke('window:toggle-maximize') as Promise<boolean>,
  closeWindow: () => ipcRenderer.invoke('window:close'),
  isMaximized: () => ipcRenderer.invoke('window:is-maximized') as Promise<boolean>,
}

contextBridge.exposeInMainWorld('electronAPI', electronBridge)
contextBridge.exposeInMainWorld('electron', electronBridge)
