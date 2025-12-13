export interface ApiError {
  code: string;
  message: string;
  detail?: string;
  fieldErrors?: Record<string, string>;
}

export interface ApiErrorResponse {
  success: false;
  error: ApiError;
  timestamp: string;
}

// 에러 코드별 사용자 메시지 매핑
export const ERROR_MESSAGES: Record<string, string> = {
  // Common
  C001: '입력값을 확인해주세요.',
  C002: '서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.',
  C003: '잘못된 형식입니다.',
  C004: '지원하지 않는 요청입니다.',
  C005: '요청한 정보를 찾을 수 없습니다.',
  
  // Authentication
  A001: '로그인이 필요합니다.',
  A002: '인증 정보가 유효하지 않습니다. 다시 로그인해주세요.',
  A003: '로그인이 만료되었습니다. 다시 로그인해주세요.',
  A004: '접근 권한이 없습니다.',
  A005: '아이디 또는 비밀번호가 일치하지 않습니다.',
  
  // User
  U001: '사용자를 찾을 수 없습니다.',
  U002: '이미 사용 중인 아이디입니다.',
  U003: '이미 사용 중인 이메일입니다.',
  U004: '비밀번호 형식이 올바르지 않습니다.',
  U005: '현재 비밀번호가 일치하지 않습니다.',
  
  // API Key
  K001: 'API 키를 먼저 등록해주세요.',
  K002: 'API 키 처리 중 오류가 발생했습니다.',
  K003: 'API 키 처리 중 오류가 발생했습니다.',
  K004: '유효하지 않은 API 키입니다.',
  
  // Trading
  T001: '거래 설정을 먼저 완료해주세요.',
  T002: '이미 거래 설정이 존재합니다.',
  T003: '거래 설정을 확인해주세요.',
  T004: '일일 거래 한도를 초과했습니다.',
  T005: '종목당 최대 보유 건수를 초과했습니다.',
  
  // Transaction
  X001: '거래 내역을 찾을 수 없습니다.',
  X002: '해당 거래에 대한 권한이 없습니다.',
  X003: '잘못된 거래 상태입니다.',
  X004: '보유 중인 자산만 매도할 수 있습니다.',
  X005: '유효한 매도 가격을 입력해주세요.',
  
  // Upbit API
  P001: '업비트 API 연결에 실패했습니다.',
  P002: '존재하지 않는 마켓입니다.',
  P003: '잔고가 부족합니다.',
  P004: '주문 처리에 실패했습니다.',
  P005: 'API 요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요.',
  P006: '업비트 점검 중입니다. 잠시 후 다시 시도해주세요.',
  
  // Notification
  N001: '알림 발송에 실패했습니다.',
  N002: '이메일 발송에 실패했습니다.',
  N003: 'Discord 알림 발송에 실패했습니다.',
  N004: '이메일을 먼저 설정해주세요.',
  
  // Backtest
  B001: '백테스트 기간을 확인해주세요.',
  B002: '백테스트 데이터가 부족합니다.',
  B003: '백테스트 실행에 실패했습니다.',
};

// 에러 메시지 조회 함수
export function getErrorMessage(code: string, fallback?: string): string {
  return ERROR_MESSAGES[code] || fallback || '오류가 발생했습니다.';
}