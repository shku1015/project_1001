// CMP-16 우편번호 검색 (카카오 우편번호 서비스). ②③의 admin-form-kit.js와 같은 동작
const POSTCODE_URL = 'https://t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js'

type PostcodeData = { zonecode: string; roadAddress: string; jibunAddress: string }
type DaumGlobal = { Postcode: new (options: { oncomplete: (data: PostcodeData) => void }) => { open: () => void } }

let loading: Promise<void> | null = null

function load(): Promise<void> {
  if ((window as unknown as { daum?: DaumGlobal }).daum?.Postcode) return Promise.resolve()
  if (!loading) {
    loading = new Promise((resolve, reject) => {
      const s = document.createElement('script')
      s.src = POSTCODE_URL
      s.onload = () => resolve()
      s.onerror = () => { loading = null; reject(new Error('우편번호 서비스를 불러올 수 없습니다.')) }
      document.head.appendChild(s)
    })
  }
  return loading
}

/** 우편번호 검색을 열고, 고르면 우편번호·주소를 돌려준다 (스크립트는 처음 누를 때 한 번만 불러온다) */
export async function openPostcode(onSelect: (zipCd: string, addr: string) => void): Promise<void> {
  await load()
  const daum = (window as unknown as { daum: DaumGlobal }).daum
  new daum.Postcode({ oncomplete: (d) => onSelect(d.zonecode, d.roadAddress || d.jibunAddress) }).open()
}
