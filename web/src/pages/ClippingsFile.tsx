import { useEffect, useState } from "react"
import { getClippings, openClippings } from "../api/clippings"
import { formatDateTime } from "../utils/formatDate"

export function ClippingsFile() {
    const [uploadedAt, setUploadedAt] = useState<string | null>(null)
    const [loading, setLoading] = useState(true)
    const [opening, setOpening] = useState(false)
    const [openError, setOpenError] = useState<string | null>(null)

    useEffect(() => {
        getClippings()
            .then((res) => setUploadedAt(res.uploadedAt))
            .catch(() => setUploadedAt(null))
            .finally(() => setLoading(false))
    }, [])

    async function handleOpen() {
        setOpening(true)
        setOpenError(null)
        try {
            await openClippings()
        } catch (e) {
            setOpenError(e instanceof Error ? e.message : "Failed to open folder")
        } finally {
            setOpening(false)
        }
    }

    return (
        <main className="max-w-[600px] mx-auto px-lg py-xl">
            <h2 className="font-headline text-headline-md text-primary mb-lg">Clippings file</h2>

            {loading && <p className="font-body text-body-md text-secondary">Loading…</p>}

            {!loading && !uploadedAt && (
                <p className="font-body text-body-md text-secondary">No clippings file has been uploaded yet.</p>
            )}

            {!loading && uploadedAt && (
                <>
                    <p className="font-body text-body-md text-on-surface mb-lg">
                        Uploaded {formatDateTime(uploadedAt)}
                    </p>
                    <button
                        type="button"
                        onClick={handleOpen}
                        disabled={opening}
                        className="bg-surface-container-lowest border border-outline-variant rounded-lg px-md py-sm text-label-md font-body text-primary hover:border-primary transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                        {opening ? "Opening…" : "Open in filesystem"}
                    </button>
                    {openError && <p className="font-body text-label-md text-error mt-sm">{openError}</p>}
                </>
            )}
        </main>
    )
}
