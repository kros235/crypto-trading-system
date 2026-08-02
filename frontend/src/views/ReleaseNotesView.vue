<template>
  <v-app>
    <the-header @toggle-drawer="sidebarRef.drawer = !sidebarRef.drawer" />
    <the-sidebar ref="sidebarRef" />

    <v-main class="bg-grey-lighten-3">
      <v-container fluid>
         <v-row>
           <v-col cols="12">
             <div class="d-flex align-center">
               <h1 class="text-h4">📢 릴리즈 노트</h1>
               <HelpButton
                 :use-dialog="true"
                 :dialog-title="helpContents.releaseNotes.title"
                 :dialog-content="helpContents.releaseNotes.content"
                 color="grey-darken-1"
                 class="ml-2"
               />
             </div>
             <p class="text-subtitle-1 text-grey mb-4">시스템 업데이트 및 변경 사항을 확인하세요</p>
           </v-col>
         </v-row>

        <!-- 검색 + 페이지당 건수 + 글 작성 버튼 -->
        <v-row class="mb-4" align="center">
          <!-- ⭐ Day 48: 카테고리 필터 -->
          <v-col cols="6" md="2">
            <v-select
              v-model="selectedCategory"
              :items="categoryOptions"
              item-title="title"
              item-value="value"
              label="카테고리"
              density="compact"
              hide-details
              variant="outlined"
              bg-color="white"
            />
          </v-col>
          <v-col cols="12" md="4">
            <v-text-field
              v-model="searchKeyword"
              label="검색 (제목, 작성자, 내용)"
              prepend-inner-icon="mdi-magnify"
              clearable
              density="compact"
              hide-details
              variant="outlined"
              bg-color="white"
              @keyup.enter="loadReleaseNotes"
              @click:clear="clearSearch"
            />
          </v-col>
          <v-col cols="6" md="1">
            <v-select
              v-model="pageSize"
              :items="[10, 20, 50]"
              label="건수"
              density="compact"
              hide-details
              variant="outlined"
              bg-color="white"
              @update:model-value="loadReleaseNotes"
            />
          </v-col>
          <v-col cols="6" md="5" class="d-flex justify-end">
            <!-- ⭐ 신규: 모두 선택 / 선택 해제 토글 버튼 (관리자 전용) -->
            <v-btn
              v-if="authStore.isAdmin"
              color="secondary"
              variant="flat"
              class="mr-2 bg-white"
              @click="toggleSelectAll"
            >
              <v-icon start>{{ isAllSelected ? 'mdi-checkbox-multiple-blank-outline' : 'mdi-checkbox-multiple-marked-outline' }}</v-icon>
              {{ isAllSelected ? '선택 해제' : '모두 선택' }}
            </v-btn>
            <!-- ⭐ 신규: 선택 글 삭제 버튼 (선택된 항목이 있을 때만 표시) -->
            <v-btn
              v-if="authStore.isAdmin && selectedIds.length > 0"
              color="error"
              variant="flat"
              class="mr-2"
              @click="confirmBulkDelete"
            >
              <v-icon start>mdi-delete-sweep</v-icon>
              선택 글 삭제 ({{ selectedIds.length }})
            </v-btn>
            <v-btn color="secondary" variant="flat" class="mr-2 bg-white" @click="loadReleaseNotes">
              <v-icon start>mdi-magnify</v-icon>
              검색
            </v-btn>
            <v-btn v-if="authStore.isAdmin" color="primary" @click="openCreateDialog">
              <v-icon start>mdi-plus</v-icon>
              새 글 작성
            </v-btn>
          </v-col>
        </v-row>

        <!-- 게시글 목록 -->
        <v-row>
          <v-col cols="12">
            <v-card>
              <v-data-table
                :headers="headers"
                :items="releaseNotes"
                :loading="loading"
                :items-per-page="pageSize"
                class="elevation-1"
                @click:row="openDetail"
                :show-select="authStore.isAdmin"
                v-model="selectedIds"
                item-value="id"
                @click:row.prevent
              >
                <!-- ⭐ 신규: 헤더의 기본 전체선택 체크박스는 숨기고 별도 버튼으로 대체 -->
                <template v-slot:header.data-table-select></template>
                <!-- ⭐ 글 번호: 순번 방식 (페이지 기반) -->
                <template v-slot:item.rowNum="{ index }">
                  {{ totalElements - ((currentPage - 1) * pageSize + index) }}
                </template>
                <!-- ⭐ Day 48: 카테고리 칩 표시 -->
                <template v-slot:item.category="{ item }">
                  <v-chip
                    :color="item.category === 'COIN' ? 'orange' : item.category === 'STOCK' ? 'blue' : 'grey'"
                    size="x-small"
                    variant="flat"
                  >
                    {{ item.category === 'COIN' ? '코인' : item.category === 'STOCK' ? '주식' : '공통' }}
                  </v-chip>
                </template>
                <!-- ⭐ 작성일: 한줄 표시 -->
                <template v-slot:item.createdAt="{ item }">
                  <span class="text-no-wrap">{{ formatDateOneLine(item.createdAt) }}</span>
                </template>
                <template v-slot:item.actions="{ item }" v-if="authStore.isAdmin">
                  <v-btn icon size="small" @click.stop="openEditDialog(item)">
                    <v-icon size="small">mdi-pencil</v-icon>
                  </v-btn>
                  <v-btn icon size="small" color="error" @click.stop="confirmDelete(item)">
                    <v-icon size="small">mdi-delete</v-icon>
                  </v-btn>
                </template>
                <template v-slot:bottom>
                  <div class="text-center pt-2">
                    <v-pagination
                      v-model="currentPage"
                      :length="totalPages"
                      @update:model-value="loadReleaseNotes"
                    />
                  </div>
                </template>
              </v-data-table>
            </v-card>
          </v-col>
        </v-row>

        <!-- 상세 보기 다이얼로그 -->
        <v-dialog v-model="detailDialog" max-width="1100">
          <v-card v-if="selectedNote">
            <v-card-title class="text-h5">
              {{ selectedNote.title }}
            </v-card-title>
            <v-card-subtitle>
              {{ selectedNote.authorName }} · {{ formatDate(selectedNote.createdAt) }}
            </v-card-subtitle>
            <v-divider />
            <v-card-text class="pa-4" style="white-space: pre-wrap;">
              {{ selectedNote.content }}
            </v-card-text>
            <v-card-actions>
              <v-spacer />
              <v-btn color="primary" @click="detailDialog = false">닫기</v-btn>
            </v-card-actions>
          </v-card>
        </v-dialog>

        <!-- 작성/수정 다이얼로그 -->
        <v-dialog v-model="formDialog" max-width="800" persistent>
          <v-card>
            <v-card-title>
              {{ isEditMode ? '게시글 수정' : '새 게시글 작성' }}
            </v-card-title>
            <v-divider />
            <v-card-text>
              <v-form ref="formRef" v-model="formValid">
                <v-text-field
                  v-model="form.title"
                  label="제목"
                  :rules="[v => !!v || '제목을 입력해주세요']"
                  counter="200"
                  maxlength="200"
                />
                <v-textarea
                  v-model="form.content"
                  label="내용"
                  :rules="[v => !!v || '내용을 입력해주세요']"
                  rows="15"
                  auto-grow
                />
                <!-- ⭐ Day 48: 카테고리 선택 -->
                <v-select
                  v-model="form.category"
                  :items="categoryOptions.filter(c => c.value !== '')"
                  item-title="title"
                  item-value="value"
                  label="카테고리"
                  :rules="[v => !!v || '카테고리를 선택해주세요']"
                  variant="outlined"
                />
              </v-form>
            </v-card-text>
            <v-card-actions>
              <v-spacer />
              <v-btn @click="formDialog = false">취소</v-btn>
              <v-btn 
                color="primary" 
                @click="submitForm" 
                :loading="submitting"
                :disabled="!formValid"
              >
                {{ isEditMode ? '수정' : '작성' }}
              </v-btn>
            </v-card-actions>
          </v-card>
        </v-dialog>

        <!-- 삭제 확인 다이얼로그 -->
        <v-dialog v-model="deleteDialog" max-width="400">
          <v-card>
            <v-card-title>게시글 삭제</v-card-title>
            <v-card-text>
              정말 이 게시글을 삭제하시겠습니까?
            </v-card-text>
            <v-card-actions>
              <v-spacer />
              <v-btn @click="deleteDialog = false">취소</v-btn>
              <v-btn color="error" @click="deleteNote" :loading="deleting">삭제</v-btn>
            </v-card-actions>
          </v-card>
        </v-dialog>

        <!-- ⭐ 신규: 선택 글 일괄 삭제 확인 다이얼로그 -->
        <v-dialog v-model="bulkDeleteDialog" max-width="400">
          <v-card>
            <v-card-title>선택 글 삭제</v-card-title>
            <v-card-text>
              선택하신 <strong>{{ selectedIds.length }}건</strong>의 게시글을 정말 삭제하시겠습니까?<br>
              삭제된 게시글은 복구할 수 없습니다.
            </v-card-text>
            <v-card-actions>
              <v-spacer />
              <v-btn @click="bulkDeleteDialog = false">취소</v-btn>
              <v-btn color="error" @click="bulkDeleteNotes" :loading="bulkDeleting">삭제</v-btn>
            </v-card-actions>
          </v-card>
        </v-dialog>
      </v-container>
    </v-main>
  </v-app>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'
import TheHeader from '@/components/TheHeader.vue'
import TheSidebar from '@/components/TheSidebar.vue'
import HelpButton from '@/components/HelpButton.vue'
import api from '@/api'

const authStore = useAuthStore()
const sidebarRef = ref()

const helpContents = {
  releaseNotes: {
    title: '📢 릴리즈 노트 안내',
    content: `
      <p class="help-intro">시스템의 업데이트 내역과 변경 사항을 확인할 수 있습니다.</p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>업데이트 내역</strong>
        <span class="help-desc">새로운 기능 추가, 버그 수정, 개선 사항 등의 변경 내역이 게시됩니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>검색 기능</strong>
        <span class="help-desc">제목, 작성자, 내용을 기준으로 검색할 수 있습니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>상세 보기</strong>
        <span class="help-desc">게시글 행을 클릭하면 전체 내용을 확인할 수 있습니다.</span></p>
      <p class="help-item"><span class="help-bullet">•</span> <strong>관리자 기능</strong>
        <span class="help-desc">관리자는 새 글 작성, 수정, 삭제가 가능합니다.</span></p>
      <p class="help-note">💡 <strong>Tip:</strong> 중요한 업데이트는 대시보드 시스템 알림에도 표시됩니다.</p>
    `
  }
}

// 상태
const loading = ref(false)
const submitting = ref(false)
const deleting = ref(false)
const releaseNotes = ref<any[]>([])
const currentPage = ref(1)
const totalPages = ref(1)
const pageSize = ref(10)

// 다이얼로그 상태
const detailDialog = ref(false)
const formDialog = ref(false)
const deleteDialog = ref(false)
const selectedNote = ref<any>(null)
const isEditMode = ref(false)
const formRef = ref()
const formValid = ref(false)

// ⭐ 신규: 체크박스 다중 선택 및 일괄 삭제 상태
const selectedIds = ref<number[]>([])
const bulkDeleteDialog = ref(false)
const bulkDeleting = ref(false)

// 검색 및 총 건수
const searchKeyword = ref('')
const totalElements = ref(0)

// 카테고리 필터 추가
const selectedCategory = ref<string>('')
const categoryOptions = [
  { title: '전체', value: '' },
  { title: '코인', value: 'COIN' },
  { title: '주식', value: 'STOCK' },
  { title: '공통', value: 'GENERAL' }
]

const form = ref({
  title: '',
  content: '',
  category: 'GENERAL'  // ⭐ Day 48: 카테고리 필드 추가
})

// 테이블 헤더
const headers = computed(() => {
  const baseHeaders = [
    { title: '번호', key: 'rowNum', width: '80px', sortable: false },
    { title: '카테고리', key: 'category', width: '100px', sortable: false },  // ⭐ Day 48: 카테고리 컬럼 추가
    { title: '제목', key: 'title' },
    { title: '작성자', key: 'authorName', width: '100px' },
    { title: '작성일', key: 'createdAt', width: '160px' }
  ]
  if (authStore.isAdmin) {
    baseHeaders.push({ title: '관리', key: 'actions', width: '120px', sortable: false })
  }
  return baseHeaders
})

// 메서드
const loadReleaseNotes = async () => {
  loading.value = true
  selectedIds.value = []  // ⭐ 신규: 목록 갱신 시 선택 초기화 (다른 페이지 항목이 선택된 채 남는 것 방지)
  try {
    const response = await api.get('/release-notes', {
      params: {
        page: currentPage.value - 1,
        size: pageSize.value,
        keyword: searchKeyword.value || undefined,
        category: selectedCategory.value || undefined  // ⭐ Day 48: 카테고리 필터 추가
      }
    })
    releaseNotes.value = response.data.content
    totalPages.value = response.data.totalPages
    totalElements.value = response.data.totalElements  // ⭐ 총 건수 저장
  } catch (error) {
    console.error('릴리즈 노트 목록 조회 실패:', error)
  } finally {
    loading.value = false
  }
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('ko-KR')
}

// 작성일 한줄 표시 함수
const formatDateOneLine = (dateStr: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

// 검색 초기화
const clearSearch = () => {
  searchKeyword.value = ''
  loadReleaseNotes()
}

const openDetail = (event: any, { item }: any) => {
  selectedNote.value = item
  detailDialog.value = true
}

const openCreateDialog = () => {
  isEditMode.value = false
  form.value = { title: '', content: '', category: 'GENERAL' }  // ⭐ Day 48: category 초기화
  formDialog.value = true
}

const openEditDialog = (note: any) => {
  isEditMode.value = true
  selectedNote.value = note
  form.value = {
    title: note.title,
    content: note.content,
    category: note.category || 'GENERAL'  // ⭐ Day 48: 기존 카테고리 반영
  }
  formDialog.value = true
}

const submitForm = async () => {
  if (!formRef.value) return
  const { valid } = await formRef.value.validate()
  if (!valid) return

  submitting.value = true
  try {
    if (isEditMode.value && selectedNote.value) {
      await api.put(`/release-notes/${selectedNote.value.id}`, form.value)
    } else {
      await api.post('/release-notes', form.value)
    }
    formDialog.value = false
    loadReleaseNotes()
  } catch (error) {
    console.error('저장 실패:', error)
  } finally {
    submitting.value = false
  }
}

const confirmDelete = (note: any) => {
  selectedNote.value = note
  deleteDialog.value = true
}

const deleteNote = async () => {
  if (!selectedNote.value) return
  
  deleting.value = true
  try {
    await api.delete(`/release-notes/${selectedNote.value.id}`)
    deleteDialog.value = false
    loadReleaseNotes()
  } catch (error) {
    console.error('삭제 실패:', error)
  } finally {
    deleting.value = false
  }
}

// ⭐ 신규: 현재 화면에 표시된 게시글 전체선택 / 선택해제 토글
const isAllSelected = computed(() =>
  releaseNotes.value.length > 0 && selectedIds.value.length === releaseNotes.value.length
)

const toggleSelectAll = () => {
  if (isAllSelected.value) {
    selectedIds.value = []
  } else {
    selectedIds.value = releaseNotes.value.map((note: any) => note.id)
  }
}

// ⭐ 신규: 선택 글 일괄 삭제
const confirmBulkDelete = () => {
  if (selectedIds.value.length === 0) return
  bulkDeleteDialog.value = true
}

const bulkDeleteNotes = async () => {
  bulkDeleting.value = true
  try {
    await api.delete('/release-notes/bulk', { data: { ids: selectedIds.value } })
    bulkDeleteDialog.value = false
    selectedIds.value = []
    loadReleaseNotes()
  } catch (error) {
    console.error('일괄 삭제 실패:', error)
  } finally {
    bulkDeleting.value = false
  }
}

// ⭐ Day 48: 카테고리 변경 시 목록 새로고침
watch(selectedCategory, () => {
  currentPage.value = 1
  loadReleaseNotes()
})

onMounted(() => {
  loadReleaseNotes()
})
</script>

<style scoped>
:deep(.v-data-table tbody tr) {
  cursor: pointer;
}
:deep(.v-data-table tbody tr:hover) {
  background-color: rgba(0, 0, 0, 0.04);
}

:deep(.help-content .help-intro) {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e0e0e0;
  color: #424242;
  font-size: 14px;
}

:deep(.help-content .help-item) {
  margin-bottom: 16px;
  padding-left: 8px;
}

:deep(.help-content .help-bullet) {
  color: #1565C0;
  font-weight: bold;
  margin-right: 6px;
}

:deep(.help-content .help-desc) {
  display: block;
  padding-left: 20px;
  margin-top: 4px;
  color: #616161;
  font-size: 13px;
}

:deep(.help-content .help-note) {
  margin-top: 16px;
  padding: 10px 12px;
  background-color: #FFF8E1;
  border-left: 3px solid #FFA000;
  border-radius: 4px;
  color: #5D4037;
  font-size: 13px;
}
</style>