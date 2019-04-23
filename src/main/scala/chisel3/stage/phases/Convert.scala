// See LICENSE for license details.

package chisel3.stage.phases

import chisel3.experimental.RunFirrtlTransform
import chisel3.internal.firrtl.Converter
import chisel3.stage.ChiselCircuitAnnotation

import firrtl.{AnnotationSeq, Transform}
import firrtl.options.{Phase, PreservesAll}
import firrtl.stage.{FirrtlCircuitAnnotation, RunFirrtlTransformAnnotation}

/** This prepares a [[ChiselCircuitAnnotation]] for compilation with FIRRTL. This does three things:
  *   - Uses [[chisel3.internal.firrtl.Converter]] to generate a [[FirrtlCircuitAnnotation]]
  *   - Extracts all [[firrtl.annotation.Annotation Annotation]]s from the [[chisel3.internal.firrtl.Circuit Circuit]]
  *   - Generates any needed [[RunFirrtlTransformAnnotation]]s from extracted [[firrtl.annotation.Annotation Annotation]]s
  */
class Convert extends Phase with PreservesAll[Phase] {

  override val prerequisites: Set[Class[Phase]] = Set(classOf[Checks], classOf[Elaborate])

  def transform(annotations: AnnotationSeq): AnnotationSeq = annotations.flatMap {
    case a: ChiselCircuitAnnotation =>
      /* Convert this Chisel Circuit to a FIRRTL Circuit */
      Some(FirrtlCircuitAnnotation(Converter.convert(a.circuit))) ++
        /* Convert all Chisel Annotations to FIRRTL Annotations */
        (a.circuit
           .annotations
           .map(_.toFirrtl)) ++
        /* Add requested FIRRTL Transforms for any Chisel Annotations which mixed in RunFirrtlTransform */
        (a.circuit
           .annotations
           .collect { case b: RunFirrtlTransform => b.transformClass }
           .distinct
           .filterNot(_ == classOf[firrtl.Transform])
           .map { c: Class[_ <: Transform] => RunFirrtlTransformAnnotation(c.newInstance()) })
    case a => Some(a)
  }

}
