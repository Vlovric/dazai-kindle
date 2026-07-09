import { useEffect, useRef, useState } from "react"
import { deleteTemplates, exportTemplates, listTemplates, uploadTemplate } from "../api/templates"
import type { SortOrder, TemplateFilter, TemplateSortField, TemplateSummary } from "../dto/templates"
import { TemplateEntryCard } from "../components/TemplateEntryCard"
import { SelectionActionBar } from "../components/SelectionActionBar"
import { ConfirmDialog } from "../components/ConfirmDialog"
import { saveFile } from "../utils/saveFile"

const FILTER_LABELS: Record<TemplateFilter, string> = {
    all: "All",
    output: "Output",
    heading: "Headings",
}

const SORT_LABELS: Record<TemplateSortField, string> = {
    lastModified: "Date modified",
    name: "Name",
}

export function Templates() {
    const [filter, setFilter] = useState<TemplateFilter>("all")
    const [sort, setSort] = useState<TemplateSortField>("lastModified")
    const [order, setOrder] = useState<SortOrder>("desc")
    const [page, setPage] = useState(0)

    const [templates, setTemplates] = useState<TemplateSummary[]>([])
    const [totalPages, setTotalPages] = useState(1)
    const [loading, setLoading] = useState(true)
    const [loadError, setLoadError] = useState<string | null>(null)

    const [selected, setSelected] = useState<Set<string>>(new Set())
    const [confirmingDelete, setConfirmingDelete] = useState(false)
    const [busy, setBusy] = useState(false)
    const [actionError, setActionError] = useState<string | null>(null)

    const [uploading, setUploading] = useState(false)
    const [uploadError, setUploadError] = useState<string | null>(null)
    const fileInputRef = useRef<HTMLInputElement>(null)

    const [reloadToken, setReloadToken] = useState(0)

    useEffect(() => {
        listTemplates(filter, sort, order, page)
            .then((res) => {
                setTemplates(res.templates)
                setTotalPages(res.totalPages)
                setSelected(new Set())
                setLoadError(null)
            })
            .catch((e) => setLoadError(e instanceof Error ? e.message : "Failed to load templates"))
            .finally(() => setLoading(false))
    }, [filter, sort, order, page, reloadToken])

    function handleFilterChange(value: TemplateFilter) {
        setFilter(value)
        setPage(0)
    }

    function handleSortChange(value: TemplateSortField) {
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

    async function handleUpload(file: File) {
        setUploading(true)
        setUploadError(null)
        try {
            await uploadTemplate(file)
            setLoading(true)
            setReloadToken((t) => t + 1)
        } catch (e) {
            setUploadError(e instanceof Error ? e.message : "Failed to upload template")
        } finally {
            setUploading(false)
        }
    }

    async function handleDelete() {
        const names = Array.from(selected)
        setBusy(true)
        setActionError(null)
        try {
            await deleteTemplates(names)
            setConfirmingDelete(false)
            setTemplates((prev) => prev.filter((t) => !selected.has(t.name)))
            setSelected(new Set())
        } catch (e) {
            setActionError(e instanceof Error ? e.message : "Failed to delete templates")
        } finally {
            setBusy(false)
        }
    }

    async function handleExport() {
        const names = Array.from(selected)
        setBusy(true)
        setActionError(null)
        try {
            const { blob, filename } = await exportTemplates(names)
            await saveFile(blob, filename)
        } catch (e) {
            setActionError(e instanceof Error ? e.message : "Failed to export templates")
        } finally {
            setBusy(false)
        }
    }

    return (
        <main className="max-w-[1280px] mx-auto px-lg py-xl">
            <div className="flex items-center justify-between mb-lg">
                <h2 className="font-headline text-headline-md text-primary">Template Management</h2>
                <button
                    type="button"
                    onClick={() => fileInputRef.current?.click()}
                    disabled={uploading}
                    className="bg-primary text-on-primary rounded-lg px-md py-sm text-label-md font-body disabled:opacity-50 disabled:cursor-not-allowed"
                >
                    {uploading ? "Uploading…" : "+ New Template"}
                </button>
                <input
                    ref={fileInputRef}
                    type="file"
                    accept=".ftl"
                    className="hidden"
                    onChange={(e) => {
                        const file = e.target.files?.[0]
                        if (file) {
                            handleUpload(file)
                        }
                        e.target.value = ""
                    }}
                />
            </div>

            {uploadError && <p className="font-body text-label-md text-error mb-md">{uploadError}</p>}

            <div className="flex items-center justify-between mb-lg gap-sm">
                <div className="flex items-center gap-sm">
                    {(Object.keys(FILTER_LABELS) as TemplateFilter[]).map((value) => (
                        <button
                            key={value}
                            type="button"
                            onClick={() => handleFilterChange(value)}
                            className={`rounded-lg px-md py-sm text-label-md font-body border transition-colors ${filter === value
                                ? "bg-secondary-container border-primary text-primary"
                                : "bg-surface-container-lowest border-outline-variant text-secondary hover:border-primary"
                                }`}
                        >
                            {FILTER_LABELS[value]}
                        </button>
                    ))}
                </div>
                <div className="flex items-center gap-sm">
                    <select
                        value={sort}
                        onChange={(e) => handleSortChange(e.target.value as TemplateSortField)}
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
            {loadError && <p className="font-body text-body-md text-error">Failed to load templates: {loadError}</p>}

            {!loading && !loadError && templates.length === 0 && (
                <p className="font-body text-body-md text-secondary">No templates yet.</p>
            )}

            {!loading && !loadError && templates.length > 0 && (
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-md">
                    {templates.map((template) => (
                        <TemplateEntryCard
                            key={template.name}
                            template={template}
                            selected={selected.has(template.name)}
                            onToggle={() => toggleSelected(template.name)}
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
                    title="Delete templates"
                    message={`Delete ${selected.size} selected ${selected.size === 1 ? "template" : "templates"}? This cannot be undone.`}
                    busy={busy}
                    onConfirm={handleDelete}
                    onCancel={() => setConfirmingDelete(false)}
                />
            )}
        </main>
    )
}
