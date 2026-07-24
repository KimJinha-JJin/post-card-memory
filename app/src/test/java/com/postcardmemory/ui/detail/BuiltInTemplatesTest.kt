package com.postcardmemory.ui.detail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 내장 템플릿은 이름 변경·덮어쓰기·삭제 대상이 아니어야 한다. 실제 방지는
 * UI에서 TemplateSource.USER인 템플릿에만 관리 메뉴를 노출하고, ViewModel의
 * rename/overwrite/delete가 항상 _userTemplates(파일에서 읽은, 즉 source가
 * 항상 USER인 목록)에서만 대상을 찾는 구조로 이뤄진다. 여기서는 그 전제가
 * 되는 데이터 자체가 무너지지 않았는지 검증한다.
 */
class BuiltInTemplatesTest {

    @Test
    fun all_haveBuiltInSource() {
        assertTrue(
            BuiltInTemplates.all.all { it.source == TemplateSource.BUILT_IN }
        )
    }

    @Test
    fun all_haveUniqueIds() {
        val ids = BuiltInTemplates.all.map { it.id }

        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun all_haveUniqueNames() {
        val names = BuiltInTemplates.all.map { it.name }

        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun containsExactlyFiveTemplates() {
        assertEquals(5, BuiltInTemplates.all.size)
    }

    @Test
    fun parsedFromFile_neverClaimsBuiltInSource() {
        // 사용자 템플릿 저장소에서 읽어온 템플릿은 항상 USER다 — 파일 내용을
        // 아무리 조작해도 BUILT_IN을 자칭할 수 있는 필드 자체가 직렬화 포맷에 없다.
        val builtIn = BuiltInTemplates.all.first()
        val fakedAsUser =
            builtIn.copy(source = TemplateSource.USER)

        val parsed = parsePostcardTemplate(fakedAsUser.serialize())

        assertEquals(TemplateSource.USER, parsed?.source)
    }
}
