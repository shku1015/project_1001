import { Modal as BsModal } from 'bootstrap'
import { useEffect, useRef, type ReactNode } from 'react'

/** Bootstrap(Tabler) 모달을 감싼 컴포넌트. open이 true일 때 보여 준다 */
export function Modal({ id, open, title, onClose, children, footer }: {
  id?: string
  open: boolean
  title: string
  onClose: () => void
  children: ReactNode
  footer: ReactNode
}) {
  const ref = useRef<HTMLDivElement>(null)
  const instance = useRef<BsModal | null>(null)
  // onClose 참조가 매 렌더 바뀌어도 setup 효과가 다시 돌지 않게 ref로 고정한다
  // (다시 돌면 보여 주던 모달이 dispose되어 백드롭이 남는다)
  const onCloseRef = useRef(onClose)
  onCloseRef.current = onClose

  useEffect(() => {
    const el = ref.current
    if (!el) return
    const modal = new BsModal(el)
    instance.current = modal
    const handleHidden = () => onCloseRef.current()
    el.addEventListener('hidden.bs.modal', handleHidden)
    return () => {
      el.removeEventListener('hidden.bs.modal', handleHidden)
      modal.dispose()
    }
  }, [])

  useEffect(() => {
    if (open) instance.current?.show()
    else instance.current?.hide()
  }, [open])

  return (
    <div className="modal modal-blur fade" id={id} tabIndex={-1} ref={ref}>
      <div className="modal-dialog modal-dialog-centered">
        <div className="modal-content">
          <div className="modal-header">
            {/* 제목 id: "reason-modal" → "reason-title" (②③ 마크업과 같게) */}
            <h5 className="modal-title" id={id ? `${id.replace(/-modal$/, '')}-title` : undefined}>{title}</h5>
            <button type="button" className="btn-close" data-bs-dismiss="modal" aria-label="닫기" />
          </div>
          {children}
          <div className="modal-footer">{footer}</div>
        </div>
      </div>
    </div>
  )
}
