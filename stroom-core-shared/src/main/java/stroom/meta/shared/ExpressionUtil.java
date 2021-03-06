package stroom.meta.shared;

import stroom.docref.DocRef;
import stroom.query.api.v2.ExpressionItem;
import stroom.query.api.v2.ExpressionOperator;
import stroom.query.api.v2.ExpressionOperator.Op;
import stroom.query.api.v2.ExpressionTerm;
import stroom.query.api.v2.ExpressionTerm.Condition;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class ExpressionUtil {
    private ExpressionUtil() {
        // Utility class.
    }

    public static ExpressionOperator createSimpleExpression() {
        return createSimpleExpression(MetaFieldNames.STATUS, Condition.EQUALS, Status.UNLOCKED.getDisplayValue());
    }

    public static ExpressionOperator createSimpleExpression(final String field, final Condition condition, final String value) {
        return new ExpressionOperator.Builder(Op.AND)
                .addTerm(field, condition, value)
                .build();
    }

    public static ExpressionOperator createStatusExpression(final Status status) {
        return new ExpressionOperator.Builder(Op.AND)
                .addTerm(MetaFieldNames.STATUS, Condition.EQUALS, status.getDisplayValue())
                .build();
    }

    public static ExpressionOperator createDataIdExpression(final long id) {
        final ExpressionOperator expression = new ExpressionOperator.Builder(Op.AND)
                .addTerm(MetaFieldNames.ID, Condition.EQUALS, String.valueOf(id))
                .addTerm(MetaFieldNames.STATUS, Condition.EQUALS, Status.UNLOCKED.getDisplayValue())
                .build();
        return expression;
    }

    public static ExpressionOperator createParentIdExpression(final long parentId) {
        final ExpressionOperator expression = new ExpressionOperator.Builder(Op.AND)
                .addTerm(MetaFieldNames.PARENT_ID, Condition.EQUALS, String.valueOf(parentId))
                .addTerm(MetaFieldNames.STATUS, Condition.EQUALS, Status.UNLOCKED.getDisplayValue())
                .build();
        return expression;
    }

    public static ExpressionOperator createTypeExpression(final String typeName) {
        return new ExpressionOperator.Builder(Op.AND)
                .addTerm(MetaFieldNames.TYPE_NAME, Condition.EQUALS, typeName)
                .addTerm(MetaFieldNames.STATUS, Condition.EQUALS, Status.UNLOCKED.getDisplayValue())
                .build();
    }

    public static ExpressionOperator createFeedExpression(final String feedName) {
        return createFeedsExpression(feedName);
    }

    public static ExpressionOperator createFeedsExpression(final String... feedNames) {
        final ExpressionOperator.Builder builder = new ExpressionOperator.Builder(Op.AND);

        if (feedNames != null) {
            if (feedNames.length == 1) {
                builder.addTerm(MetaFieldNames.FEED_NAME, Condition.EQUALS, feedNames[0]);
            } else {
                final ExpressionOperator.Builder or = new ExpressionOperator.Builder(Op.OR);
                for (final String feedName : feedNames) {
                    or.addTerm(MetaFieldNames.FEED_NAME, Condition.EQUALS, feedName);
                }
                builder.addOperator(or.build());
            }
        }

        builder.addTerm(MetaFieldNames.STATUS, Condition.EQUALS, Status.UNLOCKED.getDisplayValue());
        return builder.build();
    }


    public static ExpressionOperator createPipelineExpression(final DocRef pipelineRef) {
        return new ExpressionOperator.Builder(Op.AND)
                .addDocRefTerm(MetaFieldNames.PIPELINE_UUID, Condition.IS_DOC_REF, pipelineRef)
                .addTerm(MetaFieldNames.STATUS, Condition.EQUALS, Status.UNLOCKED.getDisplayValue())
                .build();
    }

    public static int termCount(final ExpressionOperator expressionOperator) {
        return terms(expressionOperator, null).size();
    }

    public static List<String> fields(final ExpressionOperator expressionOperator) {
        return terms(expressionOperator, null).stream().map(ExpressionTerm::getField).collect(Collectors.toList());
    }

    public static List<String> fields(final ExpressionOperator expressionOperator, final String field) {
        return terms(expressionOperator, field).stream().map(ExpressionTerm::getField).collect(Collectors.toList());
    }

    public static List<String> values(final ExpressionOperator expressionOperator) {
        return terms(expressionOperator, null).stream().map(ExpressionTerm::getValue).collect(Collectors.toList());
    }

    public static List<String> values(final ExpressionOperator expressionOperator, final String field) {
        return terms(expressionOperator, field).stream().map(ExpressionTerm::getValue).collect(Collectors.toList());
    }

    public static List<ExpressionTerm> terms(final ExpressionOperator expressionOperator, final String field) {
        final List<ExpressionTerm> terms = new ArrayList<>();
        addTerms(expressionOperator, field, terms);
        return terms;
    }

    private static void addTerms(final ExpressionOperator expressionOperator, final String field, final List<ExpressionTerm> terms) {
        if (expressionOperator != null && expressionOperator.enabled() && !Op.NOT.equals(expressionOperator.getOp())) {
            for (final ExpressionItem item : expressionOperator.getChildren()) {
                if (item.enabled()) {
                    if (item instanceof ExpressionTerm) {
                        final ExpressionTerm expressionTerm = (ExpressionTerm) item;
                        if ((field == null || field.equals(expressionTerm.getField())) && expressionTerm.getValue() != null && expressionTerm.getValue().length() > 0) {
                            terms.add(expressionTerm);
                        }
                    } else if (item instanceof ExpressionOperator) {
                        addTerms((ExpressionOperator) item, field, terms);
                    }
                }
            }
        }
    }


    public static ExpressionOperator copyOperator(final ExpressionOperator operator) {
        if (operator == null) {
            return null;
        }

        final ExpressionOperator.Builder builder = new ExpressionOperator.Builder(operator.enabled(), operator.getOp());
        operator.getChildren().forEach(item -> {
            if (item instanceof ExpressionOperator) {
                builder.addOperator(copyOperator((ExpressionOperator) item));

            } else if (item instanceof ExpressionTerm) {
                builder.addTerm(copyTerm((ExpressionTerm) item));
            }
        });
        return builder.build();
    }

    public static ExpressionTerm copyTerm(final ExpressionTerm term) {
        if (term == null) {
            return null;
        }

        final ExpressionTerm.Builder builder = new ExpressionTerm.Builder();
        builder.field(term.getField());
        builder.condition(term.getCondition());
        builder.value(term.getValue());
        builder.dictionary(term.getDictionary());
        builder.docRef(term.getDocRef());
        return builder.build();
    }
}
