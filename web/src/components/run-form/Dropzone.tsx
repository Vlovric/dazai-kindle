import { useRef, useState, type DragEvent } from "react"

export function Dropzone({ accept, hint, selectedName, uploading, onFile }: {
    accept: string
    hint: string
    selectedName: string | null
    uploading: boolean
    onFile: (file: File) => void
}) {
    const inputRef = useRef<HTMLInputElement>(null)
    const [dragActive, setDragActive] = useState(false)

    function handleDrop(e: DragEvent<HTMLDivElement>) {
        e.preventDefault()
        setDragActive(false)
        const file = e.dataTransfer.files?.[0]
        if (file) {
            onFile(file)
        }
    }

    return (
        <div
            onClick={() => inputRef.current?.click()}
            onDragOver={(e) => { e.preventDefault(); setDragActive(true) }}
            onDragLeave={() => setDragActive(false)}
            onDrop={handleDrop}
            className={`flex flex-col items-center justify-center gap-sm border-2 border-dashed rounded-lg py-xl px-lg cursor-pointer transition-colors ${dragActive ? "border-primary bg-secondary-container/40" : "border-outline-variant"
                }`}
        >
            <input
                ref={inputRef}
                type="file"
                accept={accept}
                className="hidden"
                onChange={(e) => {
                    const file = e.target.files?.[0]
                    if (file) {
                        onFile(file)
                    }
                    e.target.value = ""
                }}
            />
            <span aria-hidden="true" className="w-8 h-8 rounded-full border border-outline-variant" />
            {selectedName ? (
                <p className="font-body text-body-md text-primary">{uploading ? "Uploading…" : selectedName}</p>
            ) : (
                <p className="font-body text-body-md text-secondary text-center">
                    {uploading ? "Uploading…" : hint}
                </p>
            )}
        </div>
    )
}
