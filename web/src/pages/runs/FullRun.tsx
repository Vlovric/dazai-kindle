import { useState } from "react"
import { useNavigate } from "react-router-dom"
import { RunStep } from "../../components/run-form/RunStep"
import { ArtifactField } from "../../components/run-form/ArtifactField"
import { ClippingsField } from "../../components/run-form/ClippingsField"
import { ToggleSwitch } from "../../components/run-form/ToggleSwitch"
import { executeFull } from "../../api/execute"

export function FullRun() {
    const navigate = useNavigate()

    const [draftId, setDraftId] = useState<string | null>(null)
    const [bookRef, setBookRef] = useState<string | null>(null)
    const [calibrationRef, setCalibrationRef] = useState<string | null>(null)
    const [clippingsRef, setClippingsRef] = useState<string | null>("current")
    const [templateRef, setTemplateRef] = useState<string | null>(null)
    const [title, setTitle] = useState("")
    const [debugMode, setDebugMode] = useState(false)
    const [overwriteFyodorTemplate, setOverwriteFyodorTemplate] = useState(false)
    const [submitting, setSubmitting] = useState(false)
    const [error, setError] = useState<string | null>(null)

    const canExtract = Boolean(bookRef && calibrationRef && clippingsRef && templateRef) && !submitting

    async function handleExtract() {
        if (!bookRef || !calibrationRef || !clippingsRef || !templateRef) {
            return
        }
        setSubmitting(true)
        setError(null)
        try {
            await executeFull({
                bookRef,
                calibrationRef,
                clippingsRef,
                templateRef,
                title: title.trim() ? title.trim() : undefined,
                debugMode,
                overwriteFyodorTemplate,
            })
            navigate("/library")
        } catch (e) {
            setError(e instanceof Error ? e.message : "Run failed")
        } finally {
            setSubmitting(false)
        }
    }

    return (
        <main className="max-w-[800px] mx-auto px-lg py-xl">
            <h2 className="font-headline text-headline-md text-primary mb-xl">Configure New Full Run</h2>

            <RunStep step={1} label="Select Book">
                <ArtifactField type="book" draftId={draftId} onDraftIdResolved={setDraftId} onRefChange={setBookRef} />
            </RunStep>

            <RunStep step={2} label="Select Calibration File">
                <ArtifactField type="calibration" draftId={draftId} onDraftIdResolved={setDraftId} onRefChange={setCalibrationRef} />
            </RunStep>

            <RunStep step={3} label="Select Clippings File">
                <ClippingsField onRefChange={setClippingsRef} />
            </RunStep>

            <RunStep step={4} label="Select Output Template">
                <ArtifactField type="template" draftId={draftId} onDraftIdResolved={setDraftId} onRefChange={setTemplateRef} />
            </RunStep>

            <RunStep step={5} label="Title Of Book (Optional)">
                <input
                    type="text"
                    value={title}
                    onChange={(e) => setTitle(e.target.value)}
                    placeholder="Leave blank to use the book's own metadata"
                    className="w-full bg-surface border border-outline-variant rounded-lg px-md py-sm text-body-md font-body text-on-surface focus:outline-none focus:border-primary"
                />
            </RunStep>

            <RunStep step={6} label="Debug Mode">
                <ToggleSwitch checked={debugMode} onChange={setDebugMode} />
            </RunStep>

            <RunStep step={7} label="Overwrite Fyodor template">
                <ToggleSwitch checked={overwriteFyodorTemplate} onChange={setOverwriteFyodorTemplate} />
            </RunStep>

            <button
                onClick={handleExtract}
                disabled={!canExtract}
                className="w-full bg-primary text-on-primary rounded-lg py-md text-label-md font-body transition-opacity hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed"
            >
                {submitting ? "Extracting…" : "Extract"}
            </button>

            {error && <p className="font-body text-body-md text-error mt-md">{error}</p>}
        </main>
    )
}
