import { httpClient } from './httpClient'
import type { ApiEnvelope } from './envelope'

// Cliente REST de portada y páginas de los cuentos con escaleta.
// Los rechazos del servidor (409, 400, 404) llegan como ApiError con su motivo.
// Solo tipa el caso con escaleta: para cuentos antiguos usar getStoryPages de storyteller.ts.
//
// Equivalencia con las rutas propuestas en el sprint (base /api/v1/storyteller/{storyId}):
//   GET  /pages                              -> getStoryPagesView
//   POST /cover/generate                     -> generateCover
//   POST /pages/{number}/generate            -> generatePage ({number} = número de página)
//   POST /pages/{id}/hint                    -> sendHint ({id} = id de la fila, no el número)
//   POST /pages/{id}/regenerate              -> regenerate
//   POST /pages/{id}/versions/{v}/select     -> selectVersion
//   POST /pages/{id}/accept                  -> acceptPage (página PENDING)
//   POST /pages/{id}/accept-proposal         -> acceptProposal (reabierta; ?confirm en lugar de confirmCascade)
//   POST /pages/{id}/proposals/discard       -> discardProposals
//   GET  /pages/{id}/image                   -> getAcceptedImage (propuesta: GET /pages/{number}/image)

export type PagesStatus = 'SCRIPT_PENDING' | 'SCRIPT_ACCEPTED' | 'PAGES_ACCEPTED'
export type PageStatus = 'PENDING' | 'ACCEPTED'
export type VersionWarning = 'NONE' | 'TOO_MANY_WORDS' | 'SENTENCE_COUNT' | 'BOTH'
export type PageAction =
  | 'ACCEPT'
  | 'HINT'
  | 'REGENERATE'
  | 'SELECT_VERSION'
  | 'ACCEPT_PROPOSAL'
  | 'DISCARD_PROPOSALS'

// Contenido aceptado: el único que puede tener imagen. text es null en la portada.
export interface AcceptedContent {
  text: string | null
  scene: string
  hasImage: boolean
}

// Versión pendiente o propuesta de reapertura; nunca lleva imagen.
export interface PageVersionView {
  id: number
  text: string | null
  scene: string
  warning: VersionWarning
  current: boolean
}

// page 0 = portada. title solo en la portada.
export interface PageDetail {
  id: number
  page: number
  status: PageStatus
  beatId: number | null
  title: string | null
  accepted: AcceptedContent | null
  versions: PageVersionView[]
  proposals: PageVersionView[]
  actions: PageAction[]
}

// nextPage: 0 = portada, N = página N, null = nada que generar.
export interface StoryPagesView {
  storyId: number
  status: PagesStatus
  generating: boolean
  nextPage: number | null
  pages: PageDetail[]
}

export interface GenerationRequested {
  storyId: number
  pageNumber: number
}

export interface VersionSelected {
  storyId: number
  pageId: number
  versionId: number
  warning: VersionWarning
}

export interface PageAccepted {
  storyId: number
  pageId: number
  pageNumber: number
  status: PageStatus
}

// Si requiresConfirmation es true no se ha cambiado nada: affectedPages son las páginas posteriores que se eliminarían.
export interface ProposalAcceptResult extends PageAccepted {
  requiresConfirmation: boolean
  affectedPages: number
}

export interface ProposalsDiscarded {
  storyId: number
  pageId: number
  status: PageStatus
}

function base(storyId: number) {
  return `/api/v1/storyteller/${storyId}`
}

export function getStoryPagesView(storyId: number) {
  return httpClient.get<ApiEnvelope<StoryPagesView>>(`${base(storyId)}/pages`).then((envelope) => envelope.data)
}

export function getPageDetail(storyId: number, pageId: number) {
  return httpClient.get<ApiEnvelope<PageDetail>>(`${base(storyId)}/pages/${pageId}`).then((envelope) => envelope.data)
}

// Asíncrono: 202; el resultado llega por WebSocket.
export function generateCover(storyId: number) {
  return httpClient
    .post<ApiEnvelope<GenerationRequested>>(`${base(storyId)}/cover/generate`)
    .then((envelope) => envelope.data)
}

// Asíncrono: 202. pageNumber es el número de página (no el id).
export function generatePage(storyId: number, pageNumber: number) {
  return httpClient
    .post<ApiEnvelope<GenerationRequested>>(`${base(storyId)}/pages/${pageNumber}/generate`)
    .then((envelope) => envelope.data)
}

// Asíncrono: 202. Un hint vacío lo rechaza el servidor (400); el panel debe validarlo antes de enviar.
export function sendHint(storyId: number, pageId: number, hint: string) {
  return httpClient
    .post<ApiEnvelope<GenerationRequested>>(`${base(storyId)}/pages/${pageId}/hint`, { hint })
    .then((envelope) => envelope.data)
}

// Asíncrono: 202. Regenera sin hint.
export function regenerate(storyId: number, pageId: number) {
  return httpClient
    .post<ApiEnvelope<GenerationRequested>>(`${base(storyId)}/pages/${pageId}/regenerate`)
    .then((envelope) => envelope.data)
}

export function selectVersion(storyId: number, pageId: number, versionId: number) {
  return httpClient
    .post<ApiEnvelope<VersionSelected>>(`${base(storyId)}/pages/${pageId}/versions/${versionId}/select`)
    .then((envelope) => envelope.data)
}

// Para una página PENDING.
export function acceptPage(storyId: number, pageId: number) {
  return httpClient
    .post<ApiEnvelope<PageAccepted>>(`${base(storyId)}/pages/${pageId}/accept`)
    .then((envelope) => envelope.data)
}

// Para una página aceptada reabierta. Sin confirmar (false) y con páginas posteriores el servidor no cambia nada
// y responde requiresConfirmation=true con affectedPages; con confirm=true aplica el descarte en cascada.
export function acceptProposal(storyId: number, pageId: number, confirm = false) {
  return httpClient
    .post<ApiEnvelope<ProposalAcceptResult>>(`${base(storyId)}/pages/${pageId}/accept-proposal?confirm=${confirm}`)
    .then((envelope) => envelope.data)
}

export function discardProposals(storyId: number, pageId: number) {
  return httpClient
    .post<ApiEnvelope<ProposalsDiscarded>>(`${base(storyId)}/pages/${pageId}/proposals/discard`)
    .then((envelope) => envelope.data)
}

// Solo contenido aceptado con hasImage; 404 si no hay imagen. El llamador crea la URL (URL.createObjectURL).
export function getAcceptedImage(storyId: number, pageId: number) {
  return httpClient.getForBlob(`${base(storyId)}/pages/${pageId}/image`)
}
