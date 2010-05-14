/** */
var DailyWorkController = function(options) {
  this.model = null;
  this.options = {
    userId: null,
    workQueueElement: null,
    assignedStoriesElement: null,
    tasksWithoutStoryElement: null,
    emptyDailyWorkNoteBox: null,
    onUserLoadUpdate: function() {}
  };
  jQuery.extend(this.options, options);
  
  this.init();
  this.initialize();

  window.pageController.setMainController(this);
};
DailyWorkController.prototype = new CommonController();

DailyWorkController.prototype.pageControllerDispatch = function(event) {
  //new task is added to user's tasks without story
  if(event instanceof DynamicsEvents.AddEvent && event.getObject() instanceof TaskModel && event.getObject().getParent() instanceof BacklogModel) {
    this.model.addTask(event.getObject());
    if(event.getObject().getOriginalEstimate() > 0) {
      this.options.onUserLoadUpdate();
    }
  }
};
DailyWorkController.prototype.handleModelEvents = function(event) {
  if(event instanceof DynamicsEvents.NamedEvent) {
    var eventName = event.getEventName();
    if(eventName === "removedFromWorkQueue" || eventName === "addedToWorkQueue") {
      this.model.reloadWorkQueue(this.options.userId);
    }
  }
  //task oe/el changed
  if(event instanceof DynamicsEvents.MetricsEvent && event.getObject() instanceof TaskModel) {
    this.options.onUserLoadUpdate();
  }
  //task responsibles changed
  if(event instanceof DynamicsEvents.RelationUpdatedEvent && event.getObject() instanceof TaskModel && event.getRelation() === "user") {
    this.options.onUserLoadUpdate();
  }
};

DailyWorkController.prototype.initialize = function() {
  var me = this;
  ModelFactory.initializeFor(ModelFactory.initializeForTypes.dailyWork,
    this.options.userId,
    function(model) {
      me.model = model;
      me._showNoteBox();
      me._paintLists();
    }
  );
};

DailyWorkController.prototype._showNoteBox = function() {
  if (this.model.getWorkQueue().length === 0 &&
      this.model.getAssignedStories().length === 0 &&
      this.model.getTasksWithoutStory().length === 0) {
    this.options.emptyDailyWorkNoteBox.show();
  }
};

DailyWorkController.prototype._paintLists = function() {
  this.tasksWithoutStoryController = new DailyWorkTasksWithoutStoryController(
      this.model, this.options.tasksWithoutStoryElement, this);
  this.assignedStoriesController = new DailyWorkStoryListController(this.model,
      this.options.assignedStoriesElement, this);
  this.workQueueController = new WorkQueueController(this.model,
      this.options.workQueueElement, this, { userId: this.options.userId });
};


