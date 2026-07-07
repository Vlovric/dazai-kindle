import { useState } from "react"
import { RunStep } from "../../components/run-form/RunStep"
import { ArtifactField } from "../../components/run-form/ArtifactField"
import { ToggleSwitch } from "../../components/run-form/ToggleSwitch"
import { executeCalibration } from "../../api/execute"
import { saveFile } from "../../utils/saveFile"

export function CalibrationRun() {
    const [draftId, setDraftId] = useState<string | null>(null)
    const [bookRef, setBookRef] = useState<string | null>(null)
    const [debugMode, setDebugMode] = useState(false)
    const [submitting, setSubmitting] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const [saved, setSaved] = useState(false)

    const canGenerate = Boolean(bookRef) && !submitting

    async function handleGenerate() {
        if (!bookRef) {
            return
        }
        setSubmitting(true)
        setError(null)
        setSaved(false)
        try {
            const { blob, filename } = await executeCalibration({ bookRef, debugMode })
            await saveFile(blob, filename)
            setSaved(true)
        } catch (e) {
            setError(e instanceof Error ? e.message : "Run failed")
        } finally {
            setSubmitting(false)
        }
    }

    return (
        <main className="max-w-[800px] mx-auto px-lg py-xl">
            <h2 className="font-headline text-headline-md text-primary mb-xl">Generate Calibration File</h2>

            <RunStep step={1} label="Select Book">
                <ArtifactField type="book" draftId={draftId} onDraftIdResolved={setDraftId} onRefChange={setBookRef} />
            </RunStep>

            <RunStep step={2} label="Debug Mode">
                <ToggleSwitch checked={debugMode} onChange={setDebugMode} />
            </RunStep>

            <button
                onClick={handleGenerate}
                disabled={!canGenerate}
                className="w-full bg-primary text-on-primary rounded-lg py-md text-label-md font-body transition-opacity hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed"
            >
                {submitting ? "Generating…" : "Generate"}
            </button>

            {error && <p className="font-body text-body-md text-error mt-md">{error}</p>}

            {saved && (
                <div className="mt-md bg-surface border border-outline-variant rounded-lg px-md py-sm">
                    <p className="font-body text-body-md text-on-surface">
                        Calibration file saved. Fill in the blank Kindle locations, then upload it as
                        the calibration file (along with this same book) in a Full Run.
                    </p>
                </div>
            )}
        </main>
    )
}
