import { useEffect, useState } from "react"
import { Link, useNavigate, useParams } from "react-router-dom"
import { deleteTemplates, exportTemplates, getTemplate, previewTemplate } from "../api/templates"
import { ConfirmDialog } from "../components/ConfirmDialog"
import { saveFile } from "../utils/saveFile"

export function TemplateDetail() {
    const { name } = useParams<{ name: string }>()
    const navigate = useNavigate()

    const [content, setContent] = useState<string | null>(null)
    const [loading, setLoading] = useState(true)
    const [loadError, setLoadError] = useState<string | null>(null)

    const [rendered, setRendered] = useState<string | null>(null)
    const [renderError, setRenderError] = useState<string | null>(null)

    const [confirmingDelete, setConfirmingDelete] = useState(false)
    const [busy, setBusy] = useState(false)
    const [actionError, setActionError] = useState<string | null>(null)

    useEffect(() => {
        if (!name) {
            return
        }
        let cancelled = false

        async function load() {
            let templateContent: string
            try {
                const res = await getTemplate(name!)
                if (cancelled) {
                    return
                }
                templateContent = res.content
                setContent(templateContent)
                setLoadError(null)
            } catch (e) {
                if (!cancelled) {
                    setLoadError(e instanceof Error ? e.message : "Failed to load template")
                    setLoading(false)
                }
                return
            }

            try {
                const res = await previewTemplate(templateContent)
                if (!cancelled) {
                    setRendered(res.rendered)
                    setRenderError(null)
                }
            } catch (e) {
                if (!cancelled) {
                    setRenderError(e instanceof Error ? e.message : "Failed to render preview")
                }
            } finally {
                if (!cancelled) {
                    setLoading(false)
                }
            }
        }

        load()
        return () => { cancelled = true }
    }, [name])

    async function handleDelete() {
        if (!name) {
            return
        }
        setBusy(true)
        setActionError(null)
        try {
            await deleteTemplates([name])
            navigate("/templates")
        } catch (e) {
            setActionError(e instanceof Error ? e.message : "Failed to delete template")
            setBusy(false)
        }
    }

    async function handleExport() {
        if (!name) {
            return
        }
        setActionError(null)
        try {
            const { blob, filename } = await exportTemplates([name])
            await saveFile(blob, filename)
        } catch (e) {
            setActionError(e instanceof Error ? e.message : "Failed to export template")
        }
    }

    if (loading) {
        return (
            <main className="max-w-[1280px] mx-auto px-lg py-xl">
                <p className="font-body text-body-md text-secondary">Loading…</p>
            </main>
        )
    }

    if (loadError || content === null) {
        return (
            <main className="max-w-[1280px] mx-auto px-lg py-xl">
                <p className="font-body text-body-md text-error">Failed to load template: {loadError}</p>
            </main>
        )
    }

    return (
        <main className="max-w-[1280px] mx-auto px-lg py-xl">
            <Link to="/templates" className="font-body text-label-md text-secondary hover:text-primary transition-colors">
                &larr; Back to Templates
            </Link>

            <div className="flex items-center justify-between mt-md mb-lg gap-md">
                <h2 className="font-headline text-headline-md text-primary break-words">{name}</h2>
                <div className="flex items-center gap-sm shrink-0">
                    <button
                        type="button"
                        disabled={busy}
                        onClick={() => setConfirmingDelete(true)}
                        className="border border-error text-error rounded-lg px-md py-sm text-label-md font-body hover:bg-error-container transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                        Delete
                    </button>
                    <button
                        type="button"
                        disabled={busy}
                        onClick={handleExport}
                        className="bg-primary text-on-primary rounded-lg px-md py-sm text-label-md font-body disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                        Export
                    </button>
                </div>
            </div>

            {actionError && <p className="font-body text-label-md text-error mb-md">{actionError}</p>}

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-md">
                <div>
                    <h3 className="font-body text-label-md text-secondary mb-sm">Template content</h3>
                    <pre className="font-mono text-label-sm text-on-surface bg-surface-container-lowest border border-outline-variant rounded-lg p-md whitespace-pre-wrap break-words h-[600px] overflow-auto">
                        {content}
                    </pre>
                </div>
                <div>
                    <h3 className="font-body text-label-md text-secondary mb-sm">Template render</h3>
                    {renderError ? (
                        <div className="border border-error rounded-lg p-md h-[600px] overflow-auto">
                            <p className="font-body text-label-md text-error">{renderError}</p>
                        </div>
                    ) : (
                        <pre className="font-mono text-label-sm text-on-surface bg-surface-container-lowest border border-outline-variant rounded-lg p-md whitespace-pre-wrap break-words h-[600px] overflow-auto">
                            {rendered}
                        </pre>
                    )}
                </div>
            </div>

            {confirmingDelete && (
                <ConfirmDialog
                    title="Delete template"
                    message={`Delete "${name}"? This cannot be undone.`}
                    busy={busy}
                    onConfirm={handleDelete}
                    onCancel={() => setConfirmingDelete(false)}
                />
            )}
        </main>
    )
}
