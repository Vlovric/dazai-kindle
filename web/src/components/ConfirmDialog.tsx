import { Modal } from "./Modal"

export function ConfirmDialog({ title, message, confirmLabel = "Delete", busy, onConfirm, onCancel }: {
    title: string
    message: string
    confirmLabel?: string
    busy?: boolean
    onConfirm: () => void
    onCancel: () => void
}) {
    return (
        <Modal
            title={title}
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
                        disabled={busy}
                        onClick={onConfirm}
                        className="bg-error text-on-error rounded-lg px-md py-sm text-label-md font-body disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                        {busy ? "Deleting…" : confirmLabel}
                    </button>
                </>
            }
        >
            <p className="font-body text-body-md text-on-surface">{message}</p>
        </Modal>
    )
}
