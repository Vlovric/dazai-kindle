import { useState } from "react"
import { useNavigate } from "react-router-dom"
import { RunStep } from "../../components/run-form/RunStep"
import { ArtifactField } from "../../components/run-form/ArtifactField"
import { ToggleSwitch } from "../../components/run-form/ToggleSwitch"
import { executeHeadings } from "../../api/execute"

export function HeadingsRun() {
    const navigate = useNavigate()

    const [draftId, setDraftId] = useState<string | null>(null)
    const [bookRef, setBookRef] = useState<string | null>(null)
    const [calibrationRef, setCalibrationRef] = useState<string | null>(null)
    const [headingsTemplateRef, setHeadingsTemplateRef] = useState<string | null>(null)
    const [debugMode, setDebugMode] = useState(false)
    const [submitting, setSubmitting] = useState(false)
    const [error, setError] = useState<string | null>(null)

    const canExtract = Boolean(bookRef && calibrationRef && headingsTemplateRef) && !submitting

    async function handleExtract() {
        if (!bookRef || !calibrationRef || !headingsTemplateRef) {
            return
        }
        setSubmitting(true)
        setError(null)
        try {
            await executeHeadings({
                bookRef,
                calibrationRef,
                headingsTemplateRef,
                debugMode,
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
            <h2 className="font-headline text-headline-md text-primary mb-xl">Configure New Headings Only Run</h2>

            <RunStep step={1} label="Select Book">
                <ArtifactField type="book" draftId={draftId} onDraftIdResolved={setDraftId} onRefChange={setBookRef} />
            </RunStep>

            <RunStep step={2} label="Select Calibration File">
                <ArtifactField type="calibration" draftId={draftId} onDraftIdResolved={setDraftId} onRefChange={setCalibrationRef} />
            </RunStep>

            <RunStep step={3} label="Select Headings Output Template">
                <ArtifactField type="headingsTemplate" draftId={draftId} onDraftIdResolved={setDraftId} onRefChange={setHeadingsTemplateRef} />
            </RunStep>

            <RunStep step={4} label="Debug Mode">
                <ToggleSwitch checked={debugMode} onChange={setDebugMode} />
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
