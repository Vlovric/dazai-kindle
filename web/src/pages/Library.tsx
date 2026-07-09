import { useEffect, useMemo, useState } from "react"
import { Link } from "react-router-dom"
import { useTopBarActions } from "../hooks/useTopBarActions"
import { deleteRuns, exportRuns, listRuns } from "../api/runs"
import type { RunSortField, RunSummary, SortOrder } from "../dto/runs"
import { RunEntryCard } from "../components/RunEntryCard"
import { SelectionActionBar } from "../components/SelectionActionBar"
import { ConfirmDialog } from "../components/ConfirmDialog"
import { saveFile } from "../utils/saveFile"

const SORT_LABELS: Record<RunSortField, string> = {
    lastModified: "Date modified",
    name: "Name",
    author: "Author",
    highlights: "Highlights",
}

export function Library() {
    const [searchQuery, setSearchQuery] = useState("")
    const [sort, setSort] = useState<RunSortField>("lastModified")
    const [order, setOrder] = useState<SortOrder>("desc")
    const [page, setPage] = useState(0)

    const [runs, setRuns] = useState<RunSummary[]>([])
    const [totalPages, setTotalPages] = useState(1)
    const [loading, setLoading] = useState(true)
    const [loadError, setLoadError] = useState<string | null>(null)

    const [selected, setSelected] = useState<Set<string>>(new Set())
    const [confirmingDelete, setConfirmingDelete] = useState(false)
    const [busy, setBusy] = useState(false)
    const [actionError, setActionError] = useState<string | null>(null)

    useEffect(() => {
        listRuns(searchQuery, sort, order, page)
            .then((res) => {
                setRuns(res.runs)
                setTotalPages(res.totalPages)
                setSelected(new Set())
                setLoadError(null)
            })
            .catch((e) => setLoadError(e instanceof Error ? e.message : "Failed to load runs"))
            .finally(() => setLoading(false))
    }, [searchQuery, sort, order, page])

    function handleSearchChange(value: string) {
        setSearchQuery(value)
        setPage(0)
    }

    function handleSortChange(value: RunSortField) {
        setSort(value)
        setPage(0)
    }

    function toggleOrder() {
        setOrder((prev) => (prev === "asc" ? "desc" : "asc"))
        setPage(0)
    }

    function toggleSelected(name: string) {
        setSelected((prev) => {
            const next = new Set(prev)
            if (next.has(name)) {
                next.delete(name)
            } else {
                next.add(name)
            }
            return next
        })
    }

    async function handleDelete() {
        const names = Array.from(selected)
        setBusy(true)
        setActionError(null)
        try {
            await deleteRuns(names)
            setConfirmingDelete(false)
            setRuns((prev) => prev.filter((r) => !selected.has(r.name)))
            setSelected(new Set())
        } catch (e) {
            setActionError(e instanceof Error ? e.message : "Failed to delete runs")
        } finally {
            setBusy(false)
        }
    }

    async function handleExport() {
        const names = Array.from(selected)
        setBusy(true)
        setActionError(null)
        try {
            const { blob, filename } = await exportRuns(names)
            await saveFile(blob, filename)
        } catch (e) {
            setActionError(e instanceof Error ? e.message : "Failed to export runs")
        } finally {
            setBusy(false)
        }
    }

    const topBarActions = useMemo(() => (
        <>
            <input
                type="search"
                value={searchQuery}
                onChange={(e) => handleSearchChange(e.target.value)}
                placeholder="Search"
                className="bg-surface-container-lowest border border-outline-variant rounded-lg px-md py-sm text-body-md font-body text-on-surface placeholder:text-secondary focus:outline-none focus:border-primary w-64 mr-auto"
            />
            <Link
                to="/clippings"
                className="bg-surface-container-lowest border border-outline-variant rounded-lg px-md py-sm text-label-md font-body text-primary hover:border-primary transition-colors"
            >
                View clippings file
            </Link>
        </>
    ), [searchQuery])

    useTopBarActions(topBarActions)

    return (
        <main className="max-w-[1280px] mx-auto px-lg py-xl">
            <div className="flex items-center justify-between mb-lg">
                <h2 className="font-headline text-headline-md text-primary">Library</h2>
                <div className="flex items-center gap-sm">
                    <select
                        value={sort}
                        onChange={(e) => handleSortChange(e.target.value as RunSortField)}
                        aria-label="Sort by"
                        className="bg-surface-container-lowest border border-outline-variant rounded-lg px-md py-sm text-label-md font-body text-on-surface focus:outline-none focus:border-primary"
                    >
                        {Object.entries(SORT_LABELS).map(([value, label]) => (
                            <option key={value} value={value}>{label}</option>
                        ))}
                    </select>
                    <button
                        type="button"
                        onClick={toggleOrder}
                        aria-label={order === "asc" ? "Sort ascending" : "Sort descending"}
                        className="bg-surface-container-lowest border border-outline-variant rounded-lg px-sm py-sm text-label-md font-body text-primary hover:border-primary transition-colors"
                    >
                        {order === "asc" ? "↑" : "↓"}
                    </button>
                </div>
            </div>

            {loading && <p className="font-body text-body-md text-secondary">Loading…</p>}
            {loadError && <p className="font-body text-body-md text-error">Failed to load runs: {loadError}</p>}

            {!loading && !loadError && runs.length === 0 && (
                <p className="font-body text-body-md text-secondary">No past runs yet.</p>
            )}

            {!loading && !loadError && runs.length > 0 && (
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-md">
                    {runs.map((run) => (
                        <RunEntryCard
                            key={run.name}
                            run={run}
                            selected={selected.has(run.name)}
                            onToggle={() => toggleSelected(run.name)}
                        />
                    ))}
                </div>
            )}

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

            {totalPages > 1 && (
                <div className="flex items-center justify-end gap-sm mt-lg">
                    <span className="font-body text-label-md text-secondary">Page {page + 1} of {totalPages}</span>
                    <button
                        type="button"
                        disabled={page === 0}
                        onClick={() => setPage((p) => p - 1)}
                        className="bg-surface-container-lowest border border-outline-variant rounded-lg px-sm py-sm text-label-md font-body text-primary hover:border-primary transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                        &lt;
                    </button>
                    <button
                        type="button"
                        disabled={page + 1 >= totalPages}
                        onClick={() => setPage((p) => p + 1)}
                        className="bg-surface-container-lowest border border-outline-variant rounded-lg px-sm py-sm text-label-md font-body text-primary hover:border-primary transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                        &gt;
                    </button>
                </div>
            )}

            {confirmingDelete && (
                <ConfirmDialog
                    title="Delete runs"
                    message={`Delete ${selected.size} selected ${selected.size === 1 ? "run" : "runs"} and all their artifacts? This cannot be undone.`}
                    busy={busy}
                    onConfirm={handleDelete}
                    onCancel={() => setConfirmingDelete(false)}
                />
            )}
        </main>
    )
}
