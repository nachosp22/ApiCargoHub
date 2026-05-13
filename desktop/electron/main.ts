import { app, BrowserWindow, Menu, ipcMain } from 'electron'
import type { IpcMainInvokeEvent } from 'electron'
import { join } from 'node:path'

// The built directory structure
//
// ├─┬ dist-electron
// │ ├── main.js          > Electron main process
// │ └── preload.js       > Preload script
// ├─┬ dist
// │ └── index.html       > Electron renderer (Vue app)

process.env.DIST_ELECTRON = join(__dirname)
process.env.DIST = join(process.env.DIST_ELECTRON, '../dist')
process.env.VITE_PUBLIC = process.env.VITE_DEV_SERVER_URL
  ? join(process.env.DIST_ELECTRON, '../public')
  : process.env.DIST

let mainWindow: BrowserWindow | null = null

function getWindowFromEvent(event: IpcMainInvokeEvent): BrowserWindow | null {
  return BrowserWindow.fromWebContents(event.sender)
}

function getAppIconPath(): string {
  if (process.env.VITE_DEV_SERVER_URL) {
    return join(process.env.VITE_PUBLIC!, 'assets/brand/logo.png')
  }

  return join(process.env.DIST!, 'assets/brand/logo.png')
}

function createWindow(): void {
  mainWindow = new BrowserWindow({
    width: 1280,
    height: 800,
    minWidth: 1024,
    minHeight: 680,
    title: 'CargoHub Desktop',
    icon: getAppIconPath(),
    frame: false,
    autoHideMenuBar: true,
    webPreferences: {
      preload: join(__dirname, 'preload.js'),
      nodeIntegration: false,
      contextIsolation: true,
    },
  })

  Menu.setApplicationMenu(null)

  // Load the app
  if (process.env.VITE_DEV_SERVER_URL) {
    // Dev mode: load from Vite dev server
    mainWindow.loadURL(process.env.VITE_DEV_SERVER_URL)
  } else {
    // Production: load from built files
    mainWindow.loadFile(join(process.env.DIST!, 'index.html'))
  }

  mainWindow.on('closed', () => {
    mainWindow = null
  })
}

ipcMain.handle('window:minimize', (event) => {
  const targetWindow = getWindowFromEvent(event)
  targetWindow?.minimize()
})

ipcMain.handle('window:toggle-maximize', (event) => {
  const targetWindow = getWindowFromEvent(event)
  if (!targetWindow) return false

  if (targetWindow.isMaximized()) {
    targetWindow.unmaximize()
    return false
  }

  targetWindow.maximize()
  return true
})

ipcMain.handle('window:close', (event) => {
  const targetWindow = getWindowFromEvent(event)
  targetWindow?.close()
})

ipcMain.handle('window:is-maximized', (event) => {
  const targetWindow = getWindowFromEvent(event)
  return targetWindow?.isMaximized() ?? false
})

app.whenReady().then(createWindow)

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit()
  }
})

app.on('activate', () => {
  if (BrowserWindow.getAllWindows().length === 0) {
    createWindow()
  }
})
