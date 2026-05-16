import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { usePortesStore } from './portes'

const getMock = vi.fn()

vi.mock('@/services/api', () => ({
  api: {
    get: (...args: unknown[]) => getMock(...args),
  },
}))

describe('portes store - downloadAlbaran', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    getMock.mockReset()
    vi.restoreAllMocks()
  })

  it('descarga PDF cuando content-type es application/pdf', async () => {
    const store = usePortesStore()
    Object.defineProperty(window, 'URL', {
      value: {
        createObjectURL: vi.fn(() => 'blob:test'),
        revokeObjectURL: vi.fn(),
      },
      writable: true,
    })
    const appendSpy = vi.spyOn(document.body, 'appendChild')
    const removeSpy = vi.spyOn(document.body, 'removeChild')
    appendSpy.mockImplementation(() => null as unknown as Node)
    removeSpy.mockImplementation(() => null as unknown as Node)
    const click = vi.fn()
    vi.spyOn(document, 'createElement').mockReturnValue({ click, href: '', download: '' } as unknown as HTMLAnchorElement)

    getMock.mockResolvedValueOnce({
      data: new Blob(['pdf'], { type: 'application/pdf' }),
      headers: {
        'content-type': 'application/pdf',
        'content-disposition': 'attachment; filename="albaran.pdf"',
      },
    })

    await store.downloadAlbaran(1)

    expect(click).toHaveBeenCalled()
    expect(appendSpy).toHaveBeenCalled()
    expect(removeSpy).toHaveBeenCalled()
  })

  it('lanza error legible cuando el backend devuelve blob no PDF', async () => {
    const store = usePortesStore()
    getMock.mockResolvedValueOnce({
      data: { text: async () => 'Firma faltante' },
      headers: { 'content-type': 'text/plain' },
    })

    await expect(store.downloadAlbaran(1)).rejects.toThrow('Firma faltante')
    expect(store.error).toBe('Firma faltante')
  })
})
