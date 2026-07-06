import { useEffect, useState } from "react"
import { Modal } from "../Modal"
import { listFiles } from "../../api/files"
import type { ArtifactType, UploadedFileResponse } from "../../dto/files"

const LABEL: Record<ArtifactType, string> = {
    book: "Book",
    calibration: "Calibration",
    template: "Output Template",
    headingsTemplate: "Headings Template",
}

/**
 * "Choose from Library" popup, shared across all three run screens - matches
 * docs/doc/2 analysis/wireframes/artifact_storage.png (search + card grid +
 * Load More + Cancel/Confirm Selection).
 */
export function LibraryPickerModal({ type, initialSelected, onCancel, onConfirm }: {
    type: ArtifactType
    initialSelected: string | null
    onCancel: () => void
    onConfirm: (name: string) => void
}) {
    const [search, setSearch] = useState("")
    const [page, setPage] = useState(0)
    const [items, setItems] = useState<UploadedFileResponse[]>([])
    const [totalPages, setTotalPages] = useState(1)
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)
    const [selected, setSelected] = useState<string | null>(initialSelected)

    useEffect(() => {
        listFiles(type, search, page)
            .then((res) => {
                setItems((prev) => (page === 0 ? res.files : [...prev, ...res.files]))
                setTotalPages(res.totalPages)
                setError(null)
            })
            .catch((e) => setError(e instanceof Error ? e.message : "Failed to load runs"))
            .finally(() => setLoading(false))
    }, [type, search, page])

    function handleSearchChange(value: string) {
        setSearch(value)
        setPage(0)
        setItems([])
    }

    return (
        <Modal
            title={`Select ${LABEL[type]} file from Library`}
            onClose={onCancel}
            footer={
                <>
                    <button
                        type="button"
                        onClick={onCancel}
                        className="text-label-md font-body text-secondary hover:text-primary transition-colors"
                    >
                        Cancel
                    </button>
                    <button
                        type="button"
                        disabled={!selected}
                        onClick={() => selected && onConfirm(selected)}
                        className="bg-primary text-on-primary rounded-lg px-md py-sm text-label-md font-body disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                        Confirm Selection
                    </button>
                </>
            }
        >
            <input
                type="search"
                value={search}
                onChange={(e) => handleSearchChange(e.target.value)}
                placeholder="Search"
                className="w-full bg-surface border border-outline-variant rounded-lg px-md py-sm text-body-md font-body text-on-surface placeholder:text-secondary focus:outline-none focus:border-primary mb-md"
            />
            {error && <p className="font-body text-label-md text-error mb-sm">{error}</p>}
            {!loading && items.length === 0 && (
                <p className="font-body text-label-md text-secondary">No matching runs found.</p>
            )}
            <div className="grid grid-cols-2 gap-md">
                {items.map((item) => (
                    <button
                        key={item.name}
                        type="button"
                        onClick={() => setSelected(item.name)}
                        className={`border rounded-lg px-md py-lg text-label-md font-body text-center transition-colors ${selected === item.name
                            ? "border-primary bg-secondary-container text-on-secondary-container"
                            : "border-outline-variant hover:border-primary text-on-surface"
                            }`}
                    >
                        {item.name}
                    </button>
                ))}
            </div>
            {loading && <p className="font-body text-label-md text-secondary mt-sm">Loading…</p>}
            {!loading && page + 1 < totalPages && (
                <button
                    type="button"
                    onClick={() => setPage((p) => p + 1)}
                    className="mt-md text-label-md font-body text-primary hover:underline"
                >
                    Load More {LABEL[type]}s
                </button>
            )}
        </Modal>
    )
}
