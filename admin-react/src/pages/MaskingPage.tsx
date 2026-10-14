import { useCallback, useEffect, useState } from 'react'
import * as api from '../api/masking'
import type { MaskingPolicy } from '../api/masking'
import { ApiError } from '../api/client'
import { useMe } from '../auth/AuthContext'
import { can } from '../auth/permissions'
import { Layout } from '../components/Layout'
import { formatDateTime } from '../lib/format'

/** 항목별 [화면, 엑셀] 마스킹 켬/끔 */
type Switches = Record<string, { screen: boolean; excel: boolean }>

const switchesOf = (policies: MaskingPolicy[]): Switches =>
  Object.fromEntries(policies.map((p) => [p.fieldCd, { screen: p.screenMaskYn === 'Y', excel: p.excelMaskYn === 'Y' }]))

/** SCR-MSK-01 마스킹 설정 (① React). 바뀐 항목만 저장한다. ②③과 같은 마크업 */
export function MaskingPage() {
  const me = useMe()
  const canUpdate = can(me, 'MASKING', 'UPDATE')
  const [policies, setPolicies] = useState<MaskingPolicy[]>([])
  const [switches, setSwitches] = useState<Switches>({})
  const [notice, setNotice] = useState<string | null>(null)
  const [info, setInfo] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    const p = await api.getPolicies()
    setPolicies(p); setSwitches(switchesOf(p))
  }, [])
  useEffect(() => { load().catch((e) => setError(e instanceof ApiError ? e.displayMessage : '조회할 수 없습니다.')) }, [load])

  const save = async () => {
    setInfo(null); setError(null)
    const yn = (b: boolean) => (b ? 'Y' : 'N') as 'Y' | 'N'
    const changes = policies.filter((p) => {
      const s = switches[p.fieldCd]
      return yn(s.screen) !== p.screenMaskYn || yn(s.excel) !== p.excelMaskYn
    }).map((p) => ({ fieldCd: p.fieldCd, screenMaskYn: yn(switches[p.fieldCd].screen), excelMaskYn: yn(switches[p.fieldCd].excel), modDt: p.modDt }))
    if (!changes.length) { setInfo('바뀐 내용이 없습니다.'); return }
    if (!confirm('마스킹을 끄면 개인정보가 노출됩니다. 저장하시겠습니까?')) return
    try {
      await api.savePolicies({ policies: changes })
      await load()
      setNotice('저장되었습니다'); setTimeout(() => setNotice(null), 3000)
    } catch (e) { setError(e instanceof ApiError ? e.displayMessage : '저장할 수 없습니다.') }
  }

  const toggle = (fieldCd: string, kind: 'screen' | 'excel', on: boolean) =>
    setSwitches({ ...switches, [fieldCd]: { ...switches[fieldCd], [kind]: on } })

  return (
    <Layout title="마스킹 설정">
      <div id="notice-area">
        {notice && <div className="alert alert-success" role="status">{notice}</div>}
        {error && <div className="alert alert-danger" role="alert">{error}</div>}
      </div>
      <div className="card" id="masking-card">
        <div className="card-header"><h3 className="card-title">개인정보 항목별 마스킹</h3></div>
        <div className="table-responsive">
          <table className="table table-vcenter mb-0" id="masking-table">
            <thead><tr><th>개인정보 항목</th><th>마스킹 예</th><th className="text-center">화면 마스킹</th><th className="text-center">엑셀 마스킹</th><th>마지막 수정</th></tr></thead>
            <tbody id="masking-tbody">
              {policies.map((p) => (
                <tr className="masking-row" data-field-cd={p.fieldCd} key={p.fieldCd}>
                  <td>{p.fieldNm}</td><td><code>{p.example}</code></td>
                  {(['screen', 'excel'] as const).map((kind) => (
                    <td className="text-center" key={kind}>
                      <label className="form-check form-switch d-inline-block m-0">
                        <input className="form-check-input" type="checkbox" id={`${kind}-${p.fieldCd}`}
                               aria-label={`${p.fieldNm} ${kind === 'screen' ? '화면' : '엑셀'} 마스킹`} disabled={!canUpdate}
                               checked={switches[p.fieldCd]?.[kind] ?? false} onChange={(e) => toggle(p.fieldCd, kind, e.target.checked)} />
                      </label>
                    </td>
                  ))}
                  <td>{p.modNm || '-'} · {formatDateTime(p.modDt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="card-footer">
          <ul className="text-secondary mb-3" id="masking-note">
            <li>화면 마스킹을 끄면 사용자관리 조회 권한만 있어도 원문이 보입니다.</li>
            <li>엑셀 마스킹을 꺼도 개인정보열람 권한이 있는 관리자만 원문으로 내려받습니다.</li>
          </ul>
          <div id="masking-message">{info && <div className="alert alert-info" role="note">{info}</div>}</div>
          <div className="text-end">
            <button type="button" className="btn btn-primary" id="btn-save" disabled={!canUpdate} title={canUpdate ? undefined : '권한이 없습니다'} onClick={save}>저장</button>
          </div>
        </div>
      </div>
    </Layout>
  )
}
