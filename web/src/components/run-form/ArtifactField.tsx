import { useState } from "react"
import { ModeToggle } from "./ModeToggle"
import { Dropzone } from "./Dropzone"
import { LibraryPickerModal } from "./LibraryPickerModal"
import { uploadArtifact } from "../../api/files"
import type { ArtifactType } from "../../dto/files"

const ACCEPT: Record<ArtifactType, string> = {
    book: ".epub,.azw3,.mobi",
    calibration: ".txt",
    template: ".ftl",
    headingsTemplate: ".ftl",
}

const HINT: Record<ArtifactType, string> = {
    book: "Drag & drop your book file, or click to upload",
    calibration: "Drag & drop your calibration file, or click to upload",
    template: "Drag & drop your output template, or click to upload",
    headingsTemplate: "Drag & drop your headings template, or click to upload",
}

type Mode = "upload" | "library"

/**
 * One "select an artifact" step, shared across all three run screens
 * (full run, calibration generation, headings-only): book/calibration/
 * template/headingsTemplate all upload the same way and resolve to either
 * a freshly uploaded draft or a reused artifact from a completed run
 * (picked via the LibraryPickerModal popup).
 */
export function ArtifactField({ type, draftId, onDraftIdResolved, onRefChange }: {
    type: ArtifactType
    draftId: string | null
    onDraftIdResolved: (draftId: string) => void
    onRefChange: (ref: string | null) => void
}) {
    const [mode, setMode] = useState<Mode>("upload")
    const [selectedName, setSelectedName] = useState<string | null>(null)
    const [uploading, setUploading] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const [modalOpen, setModalOpen] = useState(false)

    function handleModeChange(next: Mode) {
        setMode(next)
        setError(null)
        if (next === "library") {
            setModalOpen(true)
        } else {
            setSelectedName(null)
            onRefChange(null)
        }
    }

    async function handleFile(file: File) {
        setUploading(true)
        setError(null)
        try {
            const response = await uploadArtifact(type, file, draftId)
            setSelectedName(response.name)
            if (response.draftId) {
                onDraftIdResolved(response.draftId)
                onRefChange(response.draftId)
            }
        } catch (e) {
            setError(e instanceof Error ? e.message : "Upload failed")
        } finally {
            setUploading(false)
        }
    }

    function handleConfirmSelection(name: string) {
        setSelectedName(name)
        onRefChange(name)
        setModalOpen(false)
    }

    return (
        <div>
            <ModeToggle
                value={mode}
                onChange={handleModeChange}
                options={[
                    { value: "upload", label: "Upload New File" },
                    { value: "library", label: "Choose from Library" },
                ]}
            />
            {mode === "upload" ? (
                <Dropzone
                    accept={ACCEPT[type]}
                    hint={HINT[type]}
                    selectedName={selectedName}
                    uploading={uploading}
                    onFile={handleFile}
                />
            ) : (
                <button
                    type="button"
                    onClick={() => setModalOpen(true)}
                    className="w-full text-left bg-surface border border-outline-variant rounded-lg px-md py-sm text-body-md font-body text-on-surface hover:border-primary transition-colors"
                >
                    {selectedName ?? "Choose from Library"}
                </button>
            )}
            {error && <p className="font-body text-label-md text-error mt-sm">{error}</p>}
            {modalOpen && (
                <LibraryPickerModal
                    type={type}
                    initialSelected={selectedName}
                    onCancel={() => setModalOpen(false)}
                    onConfirm={handleConfirmSelection}
                />
            )}
        </div>
    )
}
