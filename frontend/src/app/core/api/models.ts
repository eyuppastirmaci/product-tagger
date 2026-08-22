// TypeScript mirrors of the backend API contracts.

export type ProductStatus =
  | 'UPLOADED'
  | 'PREPROCESSED'
  | 'TAGGING'
  | 'PENDING_REVIEW'
  | 'APPROVED'
  | 'REJECTED'
  | 'FAILED';

export type ImageVariant = 'original' | 'processed' | 'thumbnail';

export type AttributeValues = Record<string, unknown>;
export type Confidences = Record<string, number>;

export interface ProductResponse {
  id: string;
  status: ProductStatus;
  categoryCode: string | null;
  attributes: AttributeValues | null;
  originalImagePath: string;
  processedImagePath: string;
  thumbnailPath: string;
  titleTr: string | null;
  titleEn: string | null;
  descriptionTr: string | null;
  descriptionEn: string | null;
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CategoryRef {
  code: string;
  nameTr: string;
  nameEn: string;
}

export interface ReviewProposal {
  revisionNo: number;
  proposedCategory: CategoryRef | null;
  attributes: AttributeValues | null;
  confidences: Confidences | null;
  modelName: string | null;
  createdAt: string;
}

export interface ReviewResponse {
  id: string;
  status: ProductStatus;
  processedImagePath: string;
  thumbnailPath: string;
  titleTr: string | null;
  titleEn: string | null;
  descriptionTr: string | null;
  descriptionEn: string | null;
  createdAt: string;
  proposal: ReviewProposal | null;
}

export interface ApproveRequest {
  categoryCode: string;
  attributes: AttributeValues;
}

export interface UpdateContentRequest {
  titleTr: string | null;
  titleEn: string | null;
  descriptionTr: string | null;
  descriptionEn: string | null;
}

export interface CategoryTree {
  code: string;
  nameTr: string;
  nameEn: string;
  leaf: boolean;
  children: CategoryTree[];
}

export interface CategorySchemaResponse {
  categoryCode: string;
  version: number;
  schema: AttributeSchema;
}

export interface AttributeSchema {
  attributes: SchemaAttribute[];
}

export interface SchemaAttribute {
  key: string;
  type: 'enum' | 'boolean' | 'text';
  required: boolean;
  multi?: boolean;
  label_tr: string;
  label_en: string;
  values?: SchemaValue[];
}

export interface SchemaValue {
  value: string;
  label_tr: string;
  label_en: string;
}
