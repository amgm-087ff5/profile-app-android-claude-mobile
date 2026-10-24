package com.example.businesscard.domain.model

/**
 * アプリの見た目(デザインシステム)の種類。利用者が設定で選ぶ。
 *
 * domain 層は見た目の中身を知らない。どの種類があるかと、既定はどれかだけを決める。
 * 実際の描き分けは ui 層(`ui/designsystem/`)が行う。
 */
enum class ThemeStyle {
    /** グレージュの壁に、下端が光るすりガラスの部品が浮く(ui/glass, ui/component) */
    EDGELIT,

    /** 明るいグレーの板に、柔らかい影の丸いボタン。縁から光がにじむ波が横切る(ui/porcelain) */
    PORCELAIN,

    /** 夜の黒い塔の足元に、絹のドレープのように波打つ淡い金の帯が重なり、帯の上の縁に光の線が灯る(ui/kinu。見本は夜の銀座の角の外観) */
    KINU,
    ;

    companion object {
        /** まだ選んでいないときの見た目(2026-10-10 決定: これまでの main と同じ Edgelit) */
        val DEFAULT = EDGELIT
    }
}
