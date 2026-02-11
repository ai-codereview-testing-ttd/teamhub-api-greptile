// MongoDB Seed Data for TeamHub
// Run with: mongosh teamhub seed-data.js

const now = new Date().toISOString();

// Clear existing data
db.organizations.deleteMany({});
db.members.deleteMany({});
db.projects.deleteMany({});
db.tasks.deleteMany({});

// Insert Organization
const orgId = "org_01HQ3XJMR5E0987654321";
db.organizations.insertOne({
  _id: orgId,
  name: "Acme Corporation",
  slug: "acme-corp",
  billingPlanId: "plan_pro",
  memberCount: 5,
  settings: {
    timezone: "America/New_York",
    currency: "USD"
  },
  createdAt: now,
  updatedAt: now,
  deletedAt: null
});

print("✓ Organization created");

// Insert Members
const members = [
  {
    _id: "user_01HQ3XK123",
    email: "john@acme.com",
    name: "John Doe",
    role: "OWNER",
    organizationId: orgId,
    avatarUrl: "https://i.pravatar.cc/150?img=1",
    invitedAt: now,
    joinedAt: now,
    deletedAt: null
  },
  {
    _id: "user_01HQ3XK456",
    email: "jane@acme.com",
    name: "Jane Smith",
    role: "ADMIN",
    organizationId: orgId,
    avatarUrl: "https://i.pravatar.cc/150?img=5",
    invitedAt: now,
    joinedAt: now,
    deletedAt: null
  },
  {
    _id: "user_01HQ3XK789",
    email: "bob@acme.com",
    name: "Bob Johnson",
    role: "MEMBER",
    organizationId: orgId,
    avatarUrl: "https://i.pravatar.cc/150?img=3",
    invitedAt: now,
    joinedAt: now,
    deletedAt: null
  },
  {
    _id: "user_01HQ3XK012",
    email: "alice@acme.com",
    name: "Alice Williams",
    role: "MEMBER",
    organizationId: orgId,
    avatarUrl: "https://i.pravatar.cc/150?img=9",
    invitedAt: now,
    joinedAt: now,
    deletedAt: null
  },
  {
    _id: "user_01HQ3XK345",
    email: "charlie@acme.com",
    name: "Charlie Brown",
    role: "VIEWER",
    organizationId: orgId,
    avatarUrl: "https://i.pravatar.cc/150?img=7",
    invitedAt: now,
    joinedAt: now,
    deletedAt: null
  }
];

db.members.insertMany(members);
print("✓ 5 members created");

// Insert Projects
const projects = [
  {
    _id: "proj_website_redesign",
    name: "Website Redesign",
    description: "Complete overhaul of the company website with modern design and improved UX",
    organizationId: orgId,
    status: "ACTIVE",
    memberIds: ["user_01HQ3XK123", "user_01HQ3XK456", "user_01HQ3XK789"],
    createdAt: now,
    updatedAt: now,
    deletedAt: null,
    createdBy: "user_01HQ3XK123"
  },
  {
    _id: "proj_mobile_app",
    name: "Mobile App Development",
    description: "Native iOS and Android apps for customer engagement",
    organizationId: orgId,
    status: "ACTIVE",
    memberIds: ["user_01HQ3XK456", "user_01HQ3XK789", "user_01HQ3XK012"],
    createdAt: now,
    updatedAt: now,
    deletedAt: null,
    createdBy: "user_01HQ3XK123"
  },
  {
    _id: "proj_crm_integration",
    name: "CRM Integration",
    description: "Integrate with Salesforce and HubSpot for better customer data",
    organizationId: orgId,
    status: "ACTIVE",
    memberIds: ["user_01HQ3XK012", "user_01HQ3XK345"],
    createdAt: now,
    updatedAt: now,
    deletedAt: null,
    createdBy: "user_01HQ3XK456"
  },
  {
    _id: "proj_q1_marketing",
    name: "Q1 Marketing Campaign",
    description: "Launch new product marketing campaign for Q1",
    organizationId: orgId,
    status: "COMPLETED",
    memberIds: ["user_01HQ3XK123", "user_01HQ3XK012"],
    createdAt: now,
    updatedAt: now,
    deletedAt: null,
    createdBy: "user_01HQ3XK123"
  }
];

db.projects.insertMany(projects);
print("✓ 4 projects created");

// Insert Tasks
const tasks = [
  // Website Redesign tasks
  {
    _id: "task_001",
    title: "Design new homepage mockups",
    description: "Create 3 design variations for the new homepage in Figma",
    projectId: "proj_website_redesign",
    assigneeId: "user_01HQ3XK456",
    status: "DONE",
    priority: "HIGH",
    dueDate: "2026-02-15",
    tags: ["design", "homepage"],
    createdAt: now,
    updatedAt: now,
    deletedAt: null,
    createdBy: "user_01HQ3XK123"
  },
  {
    _id: "task_002",
    title: "Implement responsive navigation",
    description: "Build mobile-friendly navigation menu with hamburger icon",
    projectId: "proj_website_redesign",
    assigneeId: "user_01HQ3XK789",
    status: "IN_PROGRESS",
    priority: "HIGH",
    dueDate: "2026-02-20",
    tags: ["frontend", "responsive"],
    createdAt: now,
    updatedAt: now,
    deletedAt: null,
    createdBy: "user_01HQ3XK123"
  },
  {
    _id: "task_003",
    title: "Optimize image loading",
    description: "Implement lazy loading and WebP format for all images",
    projectId: "proj_website_redesign",
    assigneeId: "user_01HQ3XK789",
    status: "TODO",
    priority: "MEDIUM",
    dueDate: "2026-02-25",
    tags: ["performance", "images"],
    createdAt: now,
    updatedAt: now,
    deletedAt: null,
    createdBy: "user_01HQ3XK456"
  },
  {
    _id: "task_004",
    title: "SEO audit and improvements",
    description: "Conduct full SEO audit and implement recommended changes",
    projectId: "proj_website_redesign",
    assigneeId: null,
    status: "TODO",
    priority: "MEDIUM",
    dueDate: "2026-03-01",
    tags: ["seo", "marketing"],
    createdAt: now,
    updatedAt: now,
    deletedAt: null,
    createdBy: "user_01HQ3XK123"
  },

  // Mobile App tasks
  {
    _id: "task_005",
    title: "Setup React Native project",
    description: "Initialize React Native project with Expo",
    projectId: "proj_mobile_app",
    assigneeId: "user_01HQ3XK789",
    status: "DONE",
    priority: "URGENT",
    dueDate: "2026-02-10",
    tags: ["setup", "react-native"],
    createdAt: now,
    updatedAt: now,
    deletedAt: null,
    createdBy: "user_01HQ3XK456"
  },
  {
    _id: "task_006",
    title: "Design app navigation flow",
    description: "Create user flow diagrams for main app navigation",
    projectId: "proj_mobile_app",
    assigneeId: "user_01HQ3XK456",
    status: "IN_REVIEW",
    priority: "HIGH",
    dueDate: "2026-02-18",
    tags: ["design", "ux"],
    createdAt: now,
    updatedAt: now,
    deletedAt: null,
    createdBy: "user_01HQ3XK456"
  },
  {
    _id: "task_007",
    title: "Implement user authentication",
    description: "Add OAuth login with Google and Apple Sign In",
    projectId: "proj_mobile_app",
    assigneeId: "user_01HQ3XK012",
    status: "IN_PROGRESS",
    priority: "URGENT",
    dueDate: "2026-02-22",
    tags: ["auth", "security"],
    createdAt: now,
    updatedAt: now,
    deletedAt: null,
    createdBy: "user_01HQ3XK789"
  },
  {
    _id: "task_008",
    title: "Build push notification system",
    description: "Integrate Firebase Cloud Messaging for push notifications",
    projectId: "proj_mobile_app",
    assigneeId: "user_01HQ3XK012",
    status: "TODO",
    priority: "HIGH",
    dueDate: "2026-02-28",
    tags: ["notifications", "firebase"],
    createdAt: now,
    updatedAt: now,
    deletedAt: null,
    createdBy: "user_01HQ3XK456"
  },

  // CRM Integration tasks
  {
    _id: "task_009",
    title: "Setup Salesforce API credentials",
    description: "Obtain and configure Salesforce API access",
    projectId: "proj_crm_integration",
    assigneeId: "user_01HQ3XK012",
    status: "DONE",
    priority: "HIGH",
    dueDate: "2026-02-12",
    tags: ["salesforce", "api"],
    createdAt: now,
    updatedAt: now,
    deletedAt: null,
    createdBy: "user_01HQ3XK456"
  },
  {
    _id: "task_010",
    title: "Build data sync pipeline",
    description: "Create ETL pipeline for bi-directional CRM data sync",
    projectId: "proj_crm_integration",
    assigneeId: "user_01HQ3XK012",
    status: "IN_PROGRESS",
    priority: "URGENT",
    dueDate: "2026-02-25",
    tags: ["backend", "etl"],
    createdAt: now,
    updatedAt: now,
    deletedAt: null,
    createdBy: "user_01HQ3XK012"
  },
  {
    _id: "task_011",
    title: "Test data synchronization",
    description: "Comprehensive testing of data sync with staging environment",
    projectId: "proj_crm_integration",
    assigneeId: null,
    status: "TODO",
    priority: "HIGH",
    dueDate: "2026-03-05",
    tags: ["testing", "qa"],
    createdAt: now,
    updatedAt: now,
    deletedAt: null,
    createdBy: "user_01HQ3XK012"
  },

  // Completed project tasks
  {
    _id: "task_012",
    title: "Launch social media campaign",
    description: "Execute multi-channel social media campaign",
    projectId: "proj_q1_marketing",
    assigneeId: "user_01HQ3XK012",
    status: "DONE",
    priority: "MEDIUM",
    dueDate: "2026-01-31",
    tags: ["marketing", "social"],
    createdAt: now,
    updatedAt: now,
    deletedAt: null,
    createdBy: "user_01HQ3XK123"
  }
];

db.tasks.insertMany(tasks);
print("✓ 12 tasks created");

print("\n=== Seed Data Summary ===");
print("Organizations: " + db.organizations.countDocuments());
print("Members: " + db.members.countDocuments());
print("Projects: " + db.projects.countDocuments());
print("Tasks: " + db.tasks.countDocuments());
print("\n✓ Database seeded successfully!");
