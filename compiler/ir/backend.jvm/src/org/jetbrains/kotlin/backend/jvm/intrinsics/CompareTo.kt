/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.codegen.BlockInfo
import org.jetbrains.kotlin.backend.jvm.codegen.ExpressionCodegen
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.codegen.AsmUtil.comparisonOperandType
import org.jetbrains.kotlin.codegen.AsmUtil.isPrimitive
import org.jetbrains.kotlin.codegen.OwnerKind
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrPrimitiveCallBase
import org.jetbrains.kotlin.ir.expressions.impl.IrUnaryPrimitiveImpl
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import java.lang.UnsupportedOperationException

class CompareTo : IntrinsicMethod() {
    private fun genInvoke(type: Type?, v: InstructionAdapter) {
        when (type) {
            Type.INT_TYPE -> v.invokestatic(IntrinsicMethods.INTRINSICS_CLASS_NAME, "compare", "(II)I", false)
            Type.LONG_TYPE -> v.invokestatic(IntrinsicMethods.INTRINSICS_CLASS_NAME, "compare", "(JJ)I", false)
            Type.FLOAT_TYPE -> v.invokestatic("java/lang/Float", "compare", "(FF)I", false)
            Type.DOUBLE_TYPE -> v.invokestatic("java/lang/Double", "compare", "(DD)I", false)
            else -> throw UnsupportedOperationException()
        }
    }

    override fun toCallable(expression: IrMemberAccessExpression, signature: JvmMethodSignature, context: JvmBackendContext): IrIntrinsicFunction {
        val parameterType = comparisonOperandType(
                expressionType(expression.dispatchReceiver ?: expression.extensionReceiver!!, context),
                signature.valueParameters.single().asmType
        )
        return IrIntrinsicFunction.create(expression, signature, context, listOf(parameterType, parameterType)) {
            genInvoke(parameterType, it)
        }
    }
}


class IrCompareTo : IntrinsicMethod() {
    override fun toCallable(expression: IrMemberAccessExpression, signature: JvmMethodSignature, context: JvmBackendContext): IrIntrinsicFunction {
        assert(expression is IrPrimitiveCallBase)
        val compareCall = expression.getValueArgument(0) as IrCall
        val args = compareCall.receiverAndArgs()
        val argTypes = args.asmTypes(context)
        val leftType = argTypes[0]
        val rightType = argTypes[1]
        val parameterType = comparisonOperandType(leftType, rightType)

        val newSignature = context.state.typeMapper.mapSignatureSkipGeneric(compareCall.descriptor, OwnerKind.IMPLEMENTATION)
        return object : IrIntrinsicFunction(compareCall, newSignature, context, listOf(parameterType, parameterType)) {
            override fun invoke(v: InstructionAdapter, codegen: ExpressionCodegen, data: BlockInfo): StackValue {
                val isPrimitiveIntrinsic = codegen.intrinsics.intrinsics.getIntrinsic(compareCall.descriptor) != null
                val operationType: Type
                val leftValue: StackValue
                val rightValue: StackValue
                if (isPrimitive(leftType) && isPrimitive(rightType) && isPrimitiveIntrinsic) {
                    operationType = comparisonOperandType(leftType, rightType)
                    leftValue = codegen.gen(args[0], operationType, data)
                    rightValue = codegen.gen(args[1], operationType,data)
                }
                else {
                    operationType = Type.INT_TYPE
                    leftValue = codegen.gen(compareCall, data)
                    rightValue = StackValue.constant(0, operationType)
                }
                val origin = expression.origin
                val token = when (origin) {
                    IrStatementOrigin.GT -> KtTokens.GT
                    IrStatementOrigin.GTEQ -> KtTokens.GTEQ
                    IrStatementOrigin.LT -> KtTokens.LT
                    IrStatementOrigin.LTEQ -> KtTokens.LTEQ
                    else -> TODO()
                }
                StackValue.cmp(token, operationType, leftValue, rightValue).put(Type.BOOLEAN_TYPE, v)
                return StackValue.onStack(Type.BOOLEAN_TYPE)
            }
        }
    }
}
