import { useState } from "react"
import { ModeToggle } from "./ModeToggle"
import { Dropzone } from "./Dropzone"
import { uploadClippings } from "../../api/files"

type Mode = "upload" | "current"

/**
 * Clippings are full-run specific and not draft-scoped like the other
 * artifacts: there's a single fixed clippings file on the server, always
 * overwritten by upload. The ref sent to /execute/full is only checked for
 * presence, so "current" is a stable sentinel regardless of mode.
 */
export function ClippingsField({ onRefChange }: {
    onRefChange: (ref: string | null) => void
}) {
    const [mode, setMode] = useState<Mode>("current")
    const [selectedName, setSelectedName] = useState<string | null>(null)
    const [uploading, setUploading] = useState(false)
    const [error, setError] = useState<string | null>(null)

    function handleModeChange(next: Mode) {
        setMode(next)
        setError(null)
        setSelectedName(null)
        onRefChange(next === "current" ? "current" : null)
    }

    async function handleFile(file: File) {
        setUploading(true)
        setError(null)
        try {
            const response = await uploadClippings(file)
            setSelectedName(response.name)
            onRefChange("current")
        } catch (e) {
            setError(e instanceof Error ? e.message : "Upload failed")
        } finally {
            setUploading(false)
        }
    }

    return (
        <div>
            <ModeToggle
                value={mode}
                onChange={handleModeChange}
                options={[
                    { value: "upload", label: "Upload New File" },
                    { value: "current", label: "Choose Current" },
                ]}
            />
            {mode === "upload" ? (
                <Dropzone
                    accept=".txt"
                    hint="Drag & drop your Kindle 'My Clippings.txt' file, or click to upload"
                    selectedName={selectedName}
                    uploading={uploading}
                    onFile={handleFile}
                />
            ) : (
                <p className="font-body text-body-md text-secondary px-md py-sm border border-outline-variant rounded-lg">
                    Using the current clippings file already on the server.
                </p>
            )}
            {error && <p className="font-body text-label-md text-error mt-sm">{error}</p>}
        </div>
    )
}
