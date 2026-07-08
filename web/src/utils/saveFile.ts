// Chromium supports letting the user pick the exact save location via an OS
// file picker (window.showSaveFilePicker). Other browsers fall back to a
// plain <a download> click, which saves to the browser's default downloads
// location instead - there's no equivalent OS picker there.
interface SaveFilePickerOptions {
    suggestedName?: string
}

interface FileSystemWritableFileStream {
    write(data: Blob): Promise<void>
    close(): Promise<void>
}

interface FileSystemFileHandle {
    createWritable(): Promise<FileSystemWritableFileStream>
}

declare global {
    interface Window {
        showSaveFilePicker?: (options?: SaveFilePickerOptions) => Promise<FileSystemFileHandle>
    }
}

export async function saveFile(blob: Blob, suggestedName: string): Promise<void> {
    if (window.showSaveFilePicker) {
        try {
            const handle = await window.showSaveFilePicker({ suggestedName })
            const writable = await handle.createWritable()
            await writable.write(blob)
            await writable.close()
            return
        } catch (e) {
            if (e instanceof DOMException && e.name === "AbortError") {
                return
            }
            throw e
        }
    }

    const url = URL.createObjectURL(blob)
    const link = document.createElement("a")
    link.href = url
    link.download = suggestedName
    link.click()
    URL.revokeObjectURL(url)
}
