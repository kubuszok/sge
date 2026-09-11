/*
 * Implicit unwrap for Nullable[Cell[T]] in method chains.
 *
 * The generated Cell class returns Nullable[Cell[T]] from some methods (padTop, padBottom,
 * colspan, etc.) while the old hand-ported API returned Cell[T] directly. The visui code
 * chains these calls extensively (`.padTop(6).padBottom(6).expandX()`), and each Nullable
 * return breaks the chain. This conversion unwraps automatically in chains.
 */
package sge.visui

import sge.scenes.scene2d.ui.Cell

given nullableCellUnwrap[T <: sge.scenes.scene2d.Actor]: Conversion[lowlevel.Nullable.Impl[Cell[T]], Cell[T]] =
  _.get
