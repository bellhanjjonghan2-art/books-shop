---
description: PRD 기반 Agent Team(기획자/프론트/백엔드)을 구성해 병렬 개발을 시작한다
argument-hint: [prd-파일경로 (선택, 기본 docs/prd.md)]
---

@agent-team-orchestration.md

위 오케스트레이션 지시서에 따라 에이전트 팀을 구성한다.

- 요구사항/PRD 문서: $ARGUMENTS
  (비어 있으면 docs/prd.md 를 사용. 그 파일도 없으면 기획자를 먼저 스폰해
   사람과 Q&A 로 PRD 부터 작성한다.)
- 너(팀장)는 직접 구현하지 말고 조정만 한다.
- teammate 모델은 모두 sonnet.
- 먼저 Phase 0 계획(역할별 담당 파일/모듈 분배)을 사람에게 제시하고,
  승인받은 뒤 진행한다.
