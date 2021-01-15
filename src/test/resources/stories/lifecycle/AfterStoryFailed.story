Lifecycle:
Before:
Scope: SCENARIO
Then I have another empty step
Scope: STORY
Given It is a step with an integer parameter 42
Scope: STEP
Given It is test with parameters
After:
Scope: SCENARIO
Then I have another empty step
Scope: STORY
Given I have a failed step
Scope: STEP
When I have parameter test

Scenario: The scenario

Given I have empty step
