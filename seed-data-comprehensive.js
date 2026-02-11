// MongoDB Comprehensive Seed Data for TeamHub
// Run with: mongosh teamhub seed-data-comprehensive.js

const now = new Date().toISOString();
const daysAgo = (days) => new Date(Date.now() - days * 24 * 60 * 60 * 1000).toISOString();

// Clear existing data
db.organizations.deleteMany({});
db.members.deleteMany({});
db.projects.deleteMany({});
db.tasks.deleteMany({});
db.billingplans.deleteMany({});

print("🗑️  Cleared existing data");

// Insert Organization
const orgId = "org_01HQ3XJMR5E0987654321";
db.organizations.insertOne({
  _id: orgId,
  name: "Acme Corporation",
  slug: "acme-corp",
  billingPlanId: "plan_pro",
  memberCount: 10,
  settings: {
    timezone: "America/New_York",
    currency: "USD",
    workingHours: { start: "09:00", end: "17:00" }
  },
  createdAt: daysAgo(180),
  updatedAt: now,
  deletedAt: null
});

print("✓ Organization created");

// Insert Billing Plan
db.billingplans.insertOne({
  _id: "plan_pro",
  name: "Professional",
  tier: "PRO",
  maxMembers: 50,
  maxProjects: 100,
  features: ["advanced_analytics", "priority_support", "custom_branding"],
  priceMonthly: 49.99,
  priceYearly: 499.99,
  createdAt: daysAgo(365),
  updatedAt: now
});

print("✓ Billing plan created");

// Insert Members (10 members)
const members = [
  {
    _id: "user_01HQ3XK123",
    email: "john@acme.com",
    name: "John Doe",
    role: "OWNER",
    organizationId: orgId,
    avatarUrl: "https://i.pravatar.cc/150?img=12",
    invitedAt: daysAgo(180),
    joinedAt: daysAgo(180),
    deletedAt: null
  },
  {
    _id: "user_01HQ3XK456",
    email: "jane@acme.com",
    name: "Jane Smith",
    role: "ADMIN",
    organizationId: orgId,
    avatarUrl: "https://i.pravatar.cc/150?img=5",
    invitedAt: daysAgo(170),
    joinedAt: daysAgo(170),
    deletedAt: null
  },
  {
    _id: "user_01HQ3XK789",
    email: "bob@acme.com",
    name: "Bob Johnson",
    role: "MEMBER",
    organizationId: orgId,
    avatarUrl: "https://i.pravatar.cc/150?img=33",
    invitedAt: daysAgo(160),
    joinedAt: daysAgo(160),
    deletedAt: null
  },
  {
    _id: "user_01HQ3XK012",
    email: "alice@acme.com",
    name: "Alice Williams",
    role: "MEMBER",
    organizationId: orgId,
    avatarUrl: "https://i.pravatar.cc/150?img=9",
    invitedAt: daysAgo(150),
    joinedAt: daysAgo(150),
    deletedAt: null
  },
  {
    _id: "user_01HQ3XK345",
    email: "charlie@acme.com",
    name: "Charlie Brown",
    role: "MEMBER",
    organizationId: orgId,
    avatarUrl: "https://i.pravatar.cc/150?img=7",
    invitedAt: daysAgo(140),
    joinedAt: daysAgo(140),
    deletedAt: null
  },
  {
    _id: "user_01HQ3XK678",
    email: "diana@acme.com",
    name: "Diana Martinez",
    role: "MEMBER",
    organizationId: orgId,
    avatarUrl: "https://i.pravatar.cc/150?img=44",
    invitedAt: daysAgo(130),
    joinedAt: daysAgo(130),
    deletedAt: null
  },
  {
    _id: "user_01HQ3XK901",
    email: "evan@acme.com",
    name: "Evan Taylor",
    role: "MEMBER",
    organizationId: orgId,
    avatarUrl: "https://i.pravatar.cc/150?img=15",
    invitedAt: daysAgo(120),
    joinedAt: daysAgo(120),
    deletedAt: null
  },
  {
    _id: "user_01HQ3XK234",
    email: "fiona@acme.com",
    name: "Fiona Chen",
    role: "MEMBER",
    organizationId: orgId,
    avatarUrl: "https://i.pravatar.cc/150?img=20",
    invitedAt: daysAgo(110),
    joinedAt: daysAgo(110),
    deletedAt: null
  },
  {
    _id: "user_01HQ3XK567",
    email: "george@acme.com",
    name: "George Wilson",
    role: "ADMIN",
    organizationId: orgId,
    avatarUrl: "https://i.pravatar.cc/150?img=52",
    invitedAt: daysAgo(100),
    joinedAt: daysAgo(100),
    deletedAt: null
  },
  {
    _id: "user_01HQ3XK890",
    email: "hannah@acme.com",
    name: "Hannah Lee",
    role: "VIEWER",
    organizationId: orgId,
    avatarUrl: "https://i.pravatar.cc/150?img=26",
    invitedAt: daysAgo(90),
    joinedAt: daysAgo(90),
    deletedAt: null
  }
];

db.members.insertMany(members);
print("✓ 10 members created");

// Insert Projects (10 projects)
const projects = [
  {
    _id: "proj_website_redesign",
    name: "Website Redesign",
    description: "Complete overhaul of the company website with modern design and improved UX",
    organizationId: orgId,
    status: "ACTIVE",
    memberIds: ["user_01HQ3XK123", "user_01HQ3XK456", "user_01HQ3XK789", "user_01HQ3XK012"],
    createdAt: daysAgo(90),
    updatedAt: daysAgo(1),
    deletedAt: null,
    createdBy: "user_01HQ3XK123"
  },
  {
    _id: "proj_mobile_app",
    name: "Mobile App Development",
    description: "Native iOS and Android apps for customer engagement",
    organizationId: orgId,
    status: "ACTIVE",
    memberIds: ["user_01HQ3XK456", "user_01HQ3XK789", "user_01HQ3XK012", "user_01HQ3XK345"],
    createdAt: daysAgo(120),
    updatedAt: daysAgo(2),
    deletedAt: null,
    createdBy: "user_01HQ3XK123"
  },
  {
    _id: "proj_crm_integration",
    name: "CRM Integration",
    description: "Integrate with Salesforce and HubSpot for better customer data",
    organizationId: orgId,
    status: "ACTIVE",
    memberIds: ["user_01HQ3XK012", "user_01HQ3XK345", "user_01HQ3XK678"],
    createdAt: daysAgo(80),
    updatedAt: daysAgo(3),
    deletedAt: null,
    createdBy: "user_01HQ3XK456"
  },
  {
    _id: "proj_api_v2",
    name: "API v2 Development",
    description: "Build next generation REST API with GraphQL support",
    organizationId: orgId,
    status: "ACTIVE",
    memberIds: ["user_01HQ3XK789", "user_01HQ3XK901", "user_01HQ3XK234"],
    createdAt: daysAgo(60),
    updatedAt: daysAgo(1),
    deletedAt: null,
    createdBy: "user_01HQ3XK567"
  },
  {
    _id: "proj_analytics_dashboard",
    name: "Analytics Dashboard",
    description: "Real-time analytics and reporting dashboard for business insights",
    organizationId: orgId,
    status: "ACTIVE",
    memberIds: ["user_01HQ3XK234", "user_01HQ3XK567", "user_01HQ3XK678"],
    createdAt: daysAgo(70),
    updatedAt: daysAgo(2),
    deletedAt: null,
    createdBy: "user_01HQ3XK456"
  },
  {
    _id: "proj_security_audit",
    name: "Security Audit & Compliance",
    description: "SOC 2 compliance and comprehensive security audit",
    organizationId: orgId,
    status: "ACTIVE",
    memberIds: ["user_01HQ3XK567", "user_01HQ3XK456", "user_01HQ3XK123"],
    createdAt: daysAgo(50),
    updatedAt: daysAgo(3),
    deletedAt: null,
    createdBy: "user_01HQ3XK123"
  },
  {
    _id: "proj_q1_marketing",
    name: "Q1 Marketing Campaign",
    description: "Launch new product marketing campaign for Q1",
    organizationId: orgId,
    status: "COMPLETED",
    memberIds: ["user_01HQ3XK123", "user_01HQ3XK012", "user_01HQ3XK678"],
    createdAt: daysAgo(150),
    updatedAt: daysAgo(30),
    deletedAt: null,
    createdBy: "user_01HQ3XK123"
  },
  {
    _id: "proj_infrastructure",
    name: "Infrastructure Modernization",
    description: "Migrate to Kubernetes and implement GitOps workflows",
    organizationId: orgId,
    status: "ACTIVE",
    memberIds: ["user_01HQ3XK901", "user_01HQ3XK789", "user_01HQ3XK345"],
    createdAt: daysAgo(100),
    updatedAt: daysAgo(5),
    deletedAt: null,
    createdBy: "user_01HQ3XK567"
  },
  {
    _id: "proj_customer_portal",
    name: "Customer Self-Service Portal",
    description: "Build customer-facing portal for account management and support",
    organizationId: orgId,
    status: "ACTIVE",
    memberIds: ["user_01HQ3XK012", "user_01HQ3XK234", "user_01HQ3XK456"],
    createdAt: daysAgo(40),
    updatedAt: daysAgo(1),
    deletedAt: null,
    createdBy: "user_01HQ3XK456"
  },
  {
    _id: "proj_ai_features",
    name: "AI-Powered Features",
    description: "Integrate ML models for predictive analytics and smart recommendations",
    organizationId: orgId,
    status: "ACTIVE",
    memberIds: ["user_01HQ3XK234", "user_01HQ3XK567", "user_01HQ3XK901"],
    createdAt: daysAgo(30),
    updatedAt: now,
    deletedAt: null,
    createdBy: "user_01HQ3XK123"
  }
];

db.projects.insertMany(projects);
print("✓ 10 projects created");

// Generate comprehensive tasks (50 tasks across all projects)
const tasks = [];
let taskId = 1;

// Helper to create task
const createTask = (title, desc, projectId, assigneeId, status, priority, daysOld, tags) => ({
  _id: `task_${String(taskId++).padStart(3, '0')}`,
  title,
  description: desc,
  projectId,
  assigneeId,
  status,
  priority,
  dueDate: new Date(Date.now() + (30 - daysOld) * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
  tags,
  createdAt: daysAgo(daysOld),
  updatedAt: daysAgo(Math.max(0, daysOld - 5)),
  deletedAt: null,
  createdBy: "user_01HQ3XK123"
});

// Website Redesign tasks (8 tasks)
tasks.push(createTask("Design homepage mockups", "Create 3 design variations for new homepage in Figma", "proj_website_redesign", "user_01HQ3XK456", "DONE", "HIGH", 45, ["design", "homepage"]));
tasks.push(createTask("Implement responsive navigation", "Build mobile-friendly navigation menu", "proj_website_redesign", "user_01HQ3XK789", "DONE", "HIGH", 40, ["frontend", "responsive"]));
tasks.push(createTask("Optimize image loading", "Implement lazy loading and WebP format", "proj_website_redesign", "user_01HQ3XK789", "IN_PROGRESS", "MEDIUM", 35, ["performance", "images"]));
tasks.push(createTask("SEO audit and improvements", "Conduct full SEO audit and implement changes", "proj_website_redesign", "user_01HQ3XK012", "IN_PROGRESS", "MEDIUM", 30, ["seo", "marketing"]));
tasks.push(createTask("Accessibility compliance", "Ensure WCAG 2.1 AA compliance across all pages", "proj_website_redesign", "user_01HQ3XK456", "TODO", "HIGH", 25, ["a11y", "compliance"]));
tasks.push(createTask("CMS migration", "Migrate content from old CMS to new platform", "proj_website_redesign", "user_01HQ3XK012", "TODO", "URGENT", 20, ["cms", "migration"]));
tasks.push(createTask("Analytics integration", "Set up GA4 and custom event tracking", "proj_website_redesign", "user_01HQ3XK789", "TODO", "MEDIUM", 15, ["analytics"]));
tasks.push(createTask("Performance testing", "Load testing and optimization for 10k concurrent users", "proj_website_redesign", "user_01HQ3XK789", "TODO", "LOW", 10, ["testing", "performance"]));

// Mobile App tasks (8 tasks)
tasks.push(createTask("Setup React Native project", "Initialize React Native project with Expo", "proj_mobile_app", "user_01HQ3XK789", "DONE", "URGENT", 80, ["setup", "react-native"]));
tasks.push(createTask("Design app navigation flow", "Create user flow diagrams for main app navigation", "proj_mobile_app", "user_01HQ3XK456", "DONE", "HIGH", 75, ["design", "ux"]));
tasks.push(createTask("Implement user authentication", "Add OAuth login with Google and Apple Sign In", "proj_mobile_app", "user_01HQ3XK012", "IN_PROGRESS", "URGENT", 70, ["auth", "security"]));
tasks.push(createTask("Build push notification system", "Integrate Firebase Cloud Messaging", "proj_mobile_app", "user_01HQ3XK012", "IN_PROGRESS", "HIGH", 65, ["notifications", "firebase"]));
tasks.push(createTask("Offline mode support", "Implement local storage and sync mechanisms", "proj_mobile_app", "user_01HQ3XK345", "IN_PROGRESS", "MEDIUM", 60, ["offline", "sync"]));
tasks.push(createTask("App store submission", "Prepare builds and submit to App Store and Play Store", "proj_mobile_app", "user_01HQ3XK456", "TODO", "HIGH", 55, ["deployment"]));
tasks.push(createTask("Beta testing program", "Recruit 100 beta testers and gather feedback", "proj_mobile_app", "user_01HQ3XK012", "TODO", "MEDIUM", 50, ["testing", "beta"]));
tasks.push(createTask("App analytics dashboard", "Build internal dashboard for app usage metrics", "proj_mobile_app", "user_01HQ3XK789", "TODO", "LOW", 45, ["analytics"]));

// CRM Integration tasks (5 tasks)
tasks.push(createTask("Setup Salesforce API credentials", "Obtain and configure Salesforce API access", "proj_crm_integration", "user_01HQ3XK012", "DONE", "HIGH", 50, ["salesforce", "api"]));
tasks.push(createTask("Build data sync pipeline", "Create ETL pipeline for bi-directional CRM sync", "proj_crm_integration", "user_01HQ3XK012", "IN_PROGRESS", "URGENT", 45, ["backend", "etl"]));
tasks.push(createTask("Test data synchronization", "Comprehensive testing of data sync", "proj_crm_integration", "user_01HQ3XK345", "TODO", "HIGH", 40, ["testing", "qa"]));
tasks.push(createTask("Error handling and retry logic", "Implement robust error handling", "proj_crm_integration", "user_01HQ3XK678", "TODO", "MEDIUM", 35, ["backend", "reliability"]));
tasks.push(createTask("Documentation and training", "Create docs and train sales team", "proj_crm_integration", "user_01HQ3XK345", "TODO", "LOW", 30, ["docs", "training"]));

// API v2 tasks (6 tasks)
tasks.push(createTask("API design specification", "Design OpenAPI 3.0 specification", "proj_api_v2", "user_01HQ3XK789", "DONE", "HIGH", 40, ["api", "design"]));
tasks.push(createTask("Implement GraphQL schema", "Build GraphQL schema with resolvers", "proj_api_v2", "user_01HQ3XK901", "IN_PROGRESS", "HIGH", 35, ["graphql", "backend"]));
tasks.push(createTask("Authentication middleware", "JWT and OAuth2 authentication", "proj_api_v2", "user_01HQ3XK234", "IN_PROGRESS", "URGENT", 30, ["auth", "security"]));
tasks.push(createTask("Rate limiting implementation", "Implement rate limiting per client", "proj_api_v2", "user_01HQ3XK789", "TODO", "MEDIUM", 25, ["backend", "security"]));
tasks.push(createTask("API documentation portal", "Build interactive API docs with Swagger UI", "proj_api_v2", "user_01HQ3XK234", "TODO", "MEDIUM", 20, ["docs"]));
tasks.push(createTask("Versioning strategy", "Implement API versioning and deprecation policy", "proj_api_v2", "user_01HQ3XK901", "TODO", "LOW", 15, ["api", "strategy"]));

// Analytics Dashboard tasks (5 tasks)
tasks.push(createTask("Design dashboard layouts", "Create wireframes for 5 key dashboards", "proj_analytics_dashboard", "user_01HQ3XK234", "DONE", "HIGH", 45, ["design", "ux"]));
tasks.push(createTask("Implement real-time data pipeline", "Setup WebSocket connections for live data", "proj_analytics_dashboard", "user_01HQ3XK567", "IN_PROGRESS", "URGENT", 40, ["backend", "realtime"]));
tasks.push(createTask("Build chart library", "Custom chart components with D3.js", "proj_analytics_dashboard", "user_01HQ3XK678", "IN_PROGRESS", "HIGH", 35, ["frontend", "charts"]));
tasks.push(createTask("Data export functionality", "Enable CSV/PDF export of reports", "proj_analytics_dashboard", "user_01HQ3XK234", "TODO", "MEDIUM", 30, ["feature"]));
tasks.push(createTask("Scheduled reports", "Automated daily/weekly email reports", "proj_analytics_dashboard", "user_01HQ3XK567", "TODO", "LOW", 25, ["automation"]));

// Security Audit tasks (4 tasks)
tasks.push(createTask("Vulnerability scanning", "Run automated security scans on all systems", "proj_security_audit", "user_01HQ3XK567", "IN_PROGRESS", "URGENT", 30, ["security", "audit"]));
tasks.push(createTask("Penetration testing", "Third-party pentest of production systems", "proj_security_audit", "user_01HQ3XK456", "TODO", "URGENT", 25, ["security", "testing"]));
tasks.push(createTask("SOC 2 documentation", "Prepare all required SOC 2 documentation", "proj_security_audit", "user_01HQ3XK123", "TODO", "HIGH", 20, ["compliance", "docs"]));
tasks.push(createTask("Security training", "Mandatory security training for all engineers", "proj_security_audit", "user_01HQ3XK567", "TODO", "MEDIUM", 15, ["training", "security"]));

// Q1 Marketing (completed project - 3 tasks all done)
tasks.push(createTask("Launch social media campaign", "Execute multi-channel social media campaign", "proj_q1_marketing", "user_01HQ3XK012", "DONE", "MEDIUM", 120, ["marketing", "social"]));
tasks.push(createTask("Email marketing sequence", "Build 5-email drip campaign", "proj_q1_marketing", "user_01HQ3XK678", "DONE", "MEDIUM", 115, ["marketing", "email"]));
tasks.push(createTask("Campaign analytics report", "Analyze campaign performance and ROI", "proj_q1_marketing", "user_01HQ3XK012", "DONE", "LOW", 110, ["analytics", "reporting"]));

// Infrastructure tasks (4 tasks)
tasks.push(createTask("Kubernetes cluster setup", "Deploy production K8s cluster on AWS EKS", "proj_infrastructure", "user_01HQ3XK901", "DONE", "URGENT", 60, ["devops", "kubernetes"]));
tasks.push(createTask("GitOps workflow implementation", "Setup ArgoCD for automated deployments", "proj_infrastructure", "user_01HQ3XK789", "IN_PROGRESS", "HIGH", 55, ["devops", "gitops"]));
tasks.push(createTask("Monitoring and alerting", "Deploy Prometheus and Grafana stack", "proj_infrastructure", "user_01HQ3XK345", "TODO", "HIGH", 50, ["monitoring", "devops"]));
tasks.push(createTask("Disaster recovery plan", "Design and test DR procedures", "proj_infrastructure", "user_01HQ3XK901", "TODO", "MEDIUM", 45, ["devops", "dr"]));

// Customer Portal tasks (4 tasks)
tasks.push(createTask("User authentication system", "Implement secure login and SSO", "proj_customer_portal", "user_01HQ3XK012", "IN_PROGRESS", "URGENT", 25, ["auth", "frontend"]));
tasks.push(createTask("Account management UI", "Build account settings and billing pages", "proj_customer_portal", "user_01HQ3XK234", "IN_PROGRESS", "HIGH", 20, ["frontend", "ui"]));
tasks.push(createTask("Support ticket system", "Integrated ticketing and chat support", "proj_customer_portal", "user_01HQ3XK456", "TODO", "MEDIUM", 15, ["feature", "support"]));
tasks.push(createTask("Knowledge base integration", "Searchable help docs and FAQs", "proj_customer_portal", "user_01HQ3XK012", "TODO", "LOW", 10, ["docs", "support"]));

// AI Features tasks (3 tasks)
tasks.push(createTask("ML model training pipeline", "Setup automated model training workflow", "proj_ai_features", "user_01HQ3XK234", "IN_PROGRESS", "HIGH", 15, ["ml", "ai"]));
tasks.push(createTask("Recommendation engine", "Build collaborative filtering recommendation system", "proj_ai_features", "user_01HQ3XK567", "TODO", "HIGH", 10, ["ml", "recommendations"]));
tasks.push(createTask("A/B testing framework", "Framework for testing ML model variants", "proj_ai_features", "user_01HQ3XK901", "TODO", "MEDIUM", 5, ["ml", "testing"]));

db.tasks.insertMany(tasks);
print(`✓ ${tasks.length} tasks created`);

// Calculate and display statistics
const tasksByStatus = db.tasks.aggregate([
  { $group: { _id: "$status", count: { $sum: 1 } } }
]).toArray();

const tasksByPriority = db.tasks.aggregate([
  { $group: { _id: "$priority", count: { $sum: 1 } } }
]).toArray();

print("\n=== Comprehensive Seed Data Summary ===");
print(`Organizations: ${db.organizations.countDocuments()}`);
print(`Members: ${db.members.countDocuments()}`);
print(`Projects: ${db.projects.countDocuments()}`);
print(`Tasks: ${db.tasks.countDocuments()}`);
print(`Billing Plans: ${db.billingplans.countDocuments()}`);

print("\nTasks by Status:");
tasksByStatus.forEach(stat => print(`  - ${stat._id}: ${stat.count}`));

print("\nTasks by Priority:");
tasksByPriority.forEach(stat => print(`  - ${stat._id}: ${stat.count}`));

print("\n✓ Comprehensive database seed complete!");
print("🎉 All screens should now have plenty of data to display");
