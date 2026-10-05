// api/openapi.yaml에서 생성한 타입(schema.d.ts)의 별칭. 직접 타입을 만들지 말고 여기서 가져다 쓴다.
import type { components } from './schema'

type Schemas = components['schemas']

export type Me = Schemas['Me']
export type MenuNode = Schemas['MenuNode']
export type Token = Schemas['Token']
export type UpdateMyInfoRequest = Schemas['UpdateMyInfoRequest']
export type ErrorCode = Schemas['ErrorCode']
export type FieldError = Schemas['FieldError']
