import { useEffect, useState } from "react"
import { Link, useParams } from "react-router-dom"
import { deleteArtifacts, exportArtifacts, getRunDetail, openArtifact } from "../api/runs"
import type { ArtifactKey, RunDetail } from "../dto/runs"
import { ArtifactRow } from "../components/ArtifactRow"
import { SelectionActionBar } from "../components/SelectionActionBar"
import { ConfirmDialog } from "../components/ConfirmDialog"
import { saveFile } from "../utils/saveFile"
import { formatDateTime } from "../utils/formatDate"

const ARTIFACT_ORDER: ArtifactKey[] = ["book", "calibration", "output", "headingsOutput", "debugRun"]

const ARTIFACT_LABELS: Record<ArtifactKey, string> = {
    book: "Book",
    calibration: "Calibration file",
    output: "Output",
    headingsOutput: "Headings only output",
    debugRun: "Debug run",
}

export function BookDetail() {
    const { name } = useParams<{ name: string }>()

    const [run, setRun] = useState<RunDetail | null>(null)
    const [loading, setLoading] = useState(true)
    const [loadError, setLoadError] = useState<string | null>(null)

    const [selected, setSelected] = useState<Set<ArtifactKey>>(new Set())
    const [confirmingDelete, setConfirmingDelete] = useState(false)
    const [busy, setBusy] = useState(false)
    const [actionError, setActionError] = useState<string | null>(null)

    useEffect(() => {
        if (!name) {
            return
        }
        getRunDetail(name)
            .then((res) => {
                setRun(res)
                setSelected(new Set())
                setLoadError(null)
            })
            .catch((e) => setLoadError(e instanceof Error ? e.message : "Failed to load run"))
            .finally(() => setLoading(false))
    }, [name])

    function toggleSelected(key: ArtifactKey) {
        setSelected((prev) => {
            const next = new Set(prev)
            if (next.has(key)) {
                next.delete(key)
            } else {
                next.add(key)
            }
            return next
        })
    }

    async function handleOpen(key: ArtifactKey) {
        if (!name) {
            return
        }
        setActionError(null)
        try {
            await openArtifact(name, key)
        } catch (e) {
            setActionError(e instanceof Error ? e.message : "Failed to open artifact")
        }
    }

    async function handleDelete() {
        if (!name || !run) {
            return
        }
        const keys = Array.from(selected)
        setBusy(true)
        setActionError(null)
        try {
            await deleteArtifacts(name, keys)
            setConfirmingDelete(false)
            setRun((prev) => {
                if (!prev) {
                    return prev
                }
                const artifacts = { ...prev.artifacts }
                keys.forEach((key) => delete artifacts[key])
                return { ...prev, artifacts }
            })
            setSelected(new Set())
        } catch (e) {
            setActionError(e instanceof Error ? e.message : "Failed to delete artifacts")
        } finally {
            setBusy(false)
        }
    }

    async function handleExport() {
        if (!name) {
            return
        }
        const keys = Array.from(selected)
        setBusy(true)
        setActionError(null)
        try {
            const { blob, filename } = await exportArtifacts(name, keys)
            await saveFile(blob, filename)
        } catch (e) {
            setActionError(e instanceof Error ? e.message : "Failed to export artifacts")
        } finally {
            setBusy(false)
        }
    }

    if (loading) {
        return (
            <main className="max-w-[800px] mx-auto px-lg py-xl">
                <p className="font-body text-body-md text-secondary">Loading…</p>
            </main>
        )
    }

    if (loadError || !run) {
        return (
            <main className="max-w-[800px] mx-auto px-lg py-xl">
                <p className="font-body text-body-md text-error">Failed to load run: {loadError}</p>
            </main>
        )
    }

    const presentArtifacts = ARTIFACT_ORDER.filter((key) => run.artifacts[key])

    return (
        <main className="max-w-[800px] mx-auto px-lg py-xl">
            <Link to="/library" className="font-body text-label-md text-secondary hover:text-primary transition-colors">
                &larr; Back to Library
            </Link>

            <h2 className="font-headline text-headline-md text-primary mt-md">{run.name}</h2>
            <p className="font-body text-body-md text-secondary">{run.author}</p>
            <div className="flex items-center gap-lg mt-sm mb-lg">
                <span className="font-body text-label-md text-secondary">{formatDateTime(run.lastModified)}</span>
                <span className="font-body text-label-md text-secondary">{run.highlightCount} highlights</span>
            </div>

            {presentArtifacts.length === 0 && (
                <p className="font-body text-body-md text-secondary">This run has no artifacts left.</p>
            )}

            <div className="flex flex-col gap-sm">
                {presentArtifacts.map((key) => (
                    <ArtifactRow
                        key={key}
                        label={ARTIFACT_LABELS[key]}
                        artifact={run.artifacts[key]!}
                        selected={selected.has(key)}
                        onToggleSelect={() => toggleSelected(key)}
                        onOpen={() => handleOpen(key)}
                    />
                ))}
            </div>

            {selected.size > 0 && (
                <SelectionActionBar
                    count={selected.size}
                    busy={busy}
                    onClear={() => setSelected(new Set())}
                    onDelete={() => setConfirmingDelete(true)}
                    onExport={handleExport}
                />
            )}

            {actionError && <p className="font-body text-label-md text-error mt-sm">{actionError}</p>}

            {confirmingDelete && (
                <ConfirmDialog
                    title="Delete artifacts"
                    message={`Delete ${selected.size} selected ${selected.size === 1 ? "artifact" : "artifacts"} from this run? This cannot be undone.`}
                    busy={busy}
                    onConfirm={handleDelete}
                    onCancel={() => setConfirmingDelete(false)}
                />
            )}
        </main>
    )
}
