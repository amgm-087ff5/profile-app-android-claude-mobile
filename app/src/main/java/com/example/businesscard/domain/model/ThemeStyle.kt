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

    /** 濡れた黒い岩の地に、苔のクッションのボタンと磨いた石の名刺 (ui/moss) */
    MOSS,
    ;

    companion object {
        /** まだ選んでいないときの見た目(2026-10-10 決定: これまでの main と同じ Edgelit) */
        val DEFAULT = EDGELIT
    }
}
