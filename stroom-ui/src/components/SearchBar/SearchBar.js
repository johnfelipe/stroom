import React from 'react';
import PropTypes from 'prop-types';
import {
  Container,
  Input,
  Button,
  Checkbox,
  Popup,
  Grid,
  TextArea,
  Form,
  Message,
} from 'semantic-ui-react';
import { compose, withState } from 'recompose';
import { connect } from 'react-redux';

import {
  ExpressionBuilder,
  actionCreators as expressionBuilderActionCreators,
} from 'components/ExpressionBuilder';

import { processSearchString } from './searchBarUtils';

const { expressionChanged } = expressionBuilderActionCreators;

const withIsExpression = withState('isExpression', 'setIsExpression', false);

const withSearchString = withState('searchString', 'setSearchString', '');
const withExpression = withState('expression', 'setExpression', '');

const withIsSearchStringValid = withState('isSearchStringValid', 'setIsSearchStringValid', true);
const withSearchStringValidationMessages = withState(
  'searchStringValidationMessages',
  'setSearchStringValidationMessages',
  [],
);

const enhance = compose(
  connect(
    (state, props) => ({
      dataSource: state.expressionBuilder.dataSources[props.expressionDataSourceUuid],
    }),
    { expressionChanged },
  ),
  withIsExpression,
  withSearchString,
  withExpression,
  withIsSearchStringValid,
  withSearchStringValidationMessages,
);

const SearchBar = ({
  expressionDataSourceUuid,
  dataSource,
  expressionId,
  expressionChanged,
  searchString,
  isExpression,
  setIsExpression,
  setSearchString,
  expression,
  setExpression,
  setIsSearchStringValid,
  isSearchStringValid,
  setSearchStringValidationMessages,
  searchStringValidationMessages,
}) => {
  const searchButton = <Button>Search</Button>;
  const searchInput = (
    <React.Fragment>
      <Grid className="SearchBar__layoutGrid">
        <Grid.Row>
          <Grid.Column width={1}>
            <Popup
              trigger={
                <Button
                  circular
                  icon="edit"
                  onClick={() => {
                    const parsedExpression = processSearchString(dataSource, searchString);
                    expressionChanged(expressionId, parsedExpression.expression);

                    setIsExpression(true);
                  }}
                />
              }
              content="Switch to using the expression builder. You won't be able to convert back to a text search and keep your expression."
            />
          </Grid.Column>
          <Grid.Column width={12}>
            <Input
              placeholder="I.e. field1=value1 field2=value2"
              error={!isSearchStringValid}
              value={searchString}
              className="SearchBar__input"
              onChange={(_, data) => {
                const expression = processSearchString(dataSource, data.value);
                const invalidFields = expression.fields.filter(field => !field.conditionIsValid || !field.fieldIsValid || !field.valueIsValid);

                const searchStringValidationMessages = [];
                if (invalidFields.length > 0) {
                  invalidFields.map((invalidField) => {
                    searchStringValidationMessages.push(`'${invalidField.original}' is not a valid search term`);
                  });
                }

                setIsSearchStringValid(invalidFields.length === 0);
                setSearchStringValidationMessages(searchStringValidationMessages);
                setSearchString(data.value);
              }}
            />
          </Grid.Column>
          <Grid.Column width={2}>{searchButton}</Grid.Column>
        </Grid.Row>
        {searchStringValidationMessages.length > 0 ? (
          <Grid.Row>
            <Grid.Column width={1} />
            <Grid.Column width={12}>
              <Container>
                <Message warning className="SearchBar__validationMessages">
                  {searchStringValidationMessages.map(message => (
                    <p>{message}</p>
                  ))}
                </Message>
              </Container>
            </Grid.Column>
            <Grid.Column width={2} />
          </Grid.Row>
        ) : (
          undefined
        )}
      </Grid>
    </React.Fragment>
  );

  const expressionBuilder = (
    <React.Fragment>
      <Grid className="SearchBar__layoutGrid">
        <Grid.Column width={1}>
          <Popup
            trigger={
              <Button
                circular
                icon="text cursor"
                className="SearchBar__modeButton"
                onClick={() => setIsExpression(false)}
              />
            }
            content="Switch to using text search. You'll lose the expression you've built here."
          />
        </Grid.Column>
        <Grid.Column width={13}>
          <ExpressionBuilder
            className="SearchBar__expressionBuilder"
            showModeToggle={false}
            editMode
            dataSourceUuid={expressionDataSourceUuid}
            expressionId={expressionId}
          />
        </Grid.Column>
        <Grid.Column width={2}>{searchButton}</Grid.Column>
      </Grid>
    </React.Fragment>
  );

  return <div className="SearchBar">{isExpression ? expressionBuilder : searchInput}</div>;
};

SearchBar.propTypes = {
  expressionDataSourceUuid: PropTypes.string.isRequired,
  expressionId: PropTypes.string.isRequired,
  searchString: PropTypes.string,
};

export default enhance(SearchBar);
